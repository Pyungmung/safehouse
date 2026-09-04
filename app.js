/**
 * SafeHouse 주거 안심 솔루션 - 메인 애플리케이션 스크립트
 * [구성 섹션]
 * 0. 서버 및 Firebase 설정 / 초기화
 * 1. 공통 토스트(Toast) 메시지 UI 제어
 * 2. FCM 웹 푸시 알림 설정 및 수신 로직
 * 3. 카카오 우편번호 주소 검색
 * 4. [탭 3] 내 모니터링 매물 CRUD (REST API 연동)
 * 5. 등기 변동 알림 내역 조회 및 렌더링
 * 6. 탭 메뉴 전환 제어
 * 7. [탭 1] 주거비 절약 진단, Chart.js 막대 그래프 & 카카오 맵 시각화
 * 8. [탭 2] 등기부 PDF 위험도 분석 리포트
 * 9. [탭 4] 회원가입 / 소셜 로그인 및 마이페이지 제어
 * 10. 포트원(아임포트) 결제 모달 및 테스트 결제 로직
 */

// ==========================================
// 0. 서버 API 및 Firebase 설정
// ==========================================
const API_BASE_URL = 'http://localhost:8080/api/v1/watchlist';
const NOTIFICATION_API_URL = 'http://localhost:8080/api/v1/notifications';
const PAYMENT_VERIFY_URL = 'http://localhost:8080/api/v1/payments/verify';

// Firebase 설정 객체
const firebaseConfig = {
    apiKey: "AIzaSyB8SZ2dQTOfyOt8xxugkSu8Oew3DmZRUFY",
    authDomain: "safehose-61162.firebaseapp.com",
    projectId: "safehose-61162",
    storageBucket: "safehose-61162.firebasestorage.app",
    messagingSenderId: "985908130594",
    appId: "1:985908130594:web:58447b3314daeba0b7eb7b",
    measurementId: "G-X9GME64SW4"
};

// Firebase 초기화
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// 포트원 결제 객체 초기화 (const 선언을 제거하여 중복 선언 에러 방지)
if (window.IMP) {
    window.IMP.init("imp00000000"); // 포트원 가맹점 식별코드 (기본 테스트 가맹점)
}

// Chart.js 전역 인스턴스 변수
let savingsChartInstance = null;

// 페이지 화면 로드 완료 시 실행
document.addEventListener('DOMContentLoaded', () => {
    fetchWatchlist();
    fetchNotificationHistory();
    requestNotificationPermission();
    initAuthEvents();
    initPaymentEvents(); // 결제 이벤트 초기화
});

// ==========================================
// 1. 공통 토스트(Toast) 메시지 제어
// ==========================================
function showToast(message) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.innerText = message;
    toast.style.display = 'block';
    toast.style.opacity = '1';

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => {
            toast.style.display = 'none';
        }, 300);
    }, 2500);
}

// ==========================================
// 2. FCM 웹 푸시 알림 권한 및 수신 제어
// ==========================================
async function requestNotificationPermission() {
    try {
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') return;
        
        const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
        const currentToken = await messaging.getToken({
            vapidKey: 'BEhcf3E_hcLO5xRj0VL8fWTgb_j8Jhyfr1zLcg7cTwgUycagWHTxsjjkU-X5EI1BPnHlNNyd3AInL2i4QhzONHA',
            serviceWorkerRegistration: registration
        });
        
        if (currentToken) {
            await sendTokenToBackend(currentToken);
        }
    } catch (error) {
        console.error('푸시 알림 설정 에러:', error);
    }
}

async function sendTokenToBackend(token) {
    try {
        await fetch('http://localhost:8080/api/v1/users/fcm-token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fcmToken: token })
        });
    } catch (error) {
        console.warn('백엔드 미연동 상태: FCM 토큰 전송을 스킵합니다.');
    }
}

messaging.onMessage((payload) => {
    alert(`[등기 변동 알림]\n${payload.notification?.title || ''}\n${payload.notification?.body || ''}`);
    fetchNotificationHistory();
});

// ==========================================
// 3. 카카오 주소 검색 기능
// ==========================================
const searchAddressBtn = document.getElementById('searchAddressBtn');
const addressInput = document.getElementById('address');
const uniqueNumberInput = document.getElementById('uniqueNumber');

if (searchAddressBtn) {
    searchAddressBtn.addEventListener('click', () => {
        new daum.Postcode({
            oncomplete: function (data) {
                let fullAddress = data.roadAddress;
                let extraAddress = '';
                
                if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)) {
                    extraAddress += data.bname;
                }
                if (data.buildingName !== '' && data.apartment === 'Y') {
                    extraAddress += (extraAddress !== '' ? ', ' + data.buildingName : data.buildingName);
                }
                if (extraAddress !== '') {
                    fullAddress += ` (${extraAddress})`;
                }
                
                if (addressInput) addressInput.value = fullAddress;
                if (uniqueNumberInput) uniqueNumberInput.focus();
            }
        }).open();
    });
}

// ==========================================
// 4. [탭 3] 내 모니터링 매물 CRUD
// ==========================================
const registerForm = document.getElementById('registerForm');
const watchlist = document.getElementById('watchlist');

let localWatchlist = [
    { id: 1, address: '서울시 강남구 역삼동 123-45', uniqueNumber: '1101-2023-012345', status: 'ACTIVE', lastCheckedAt: '2026-09-03' },
    { id: 2, address: '서울시 마포구 서교동 456-78', uniqueNumber: '1101-2023-678901', status: 'ACTIVE', lastCheckedAt: '2026-09-02' }
];

async function fetchWatchlist() {
    try {
        const response = await fetch(API_BASE_URL);
        if (!response.ok) throw new Error('서버 응답 오류');
        const data = await response.json();
        renderWatchlist(data);
    } catch (error) {
        renderWatchlist(localWatchlist);
    }
}

function renderWatchlist(items) {
    if (!watchlist) return;
    watchlist.innerHTML = '';
    
    if (!items || items.length === 0) {
        watchlist.innerHTML = `<li style="text-align: center; padding: 20px; color: #718096;">등록된 모니터링 매물이 없습니다.</li>`;
        return;
    }
    
    items.forEach(item => {
        const li = document.createElement('li');
        li.className = 'watchlist-item';
        li.setAttribute('data-id', item.id);
        
        const statusBadge = item.status === 'ACTIVE'
            ? '<span class="badge badge-active">모니터링 중</span>'
            : '<span class="badge badge-warning">중단됨</span>';
        
        li.innerHTML = `
        <div class="item-info">
            <div class="item-address">
                ${item.addressCode || item.address}
                ${statusBadge}
            </div>
            <div class="item-meta">
                고유번호: ${item.uniqueNumber} | 최근 확인: ${item.lastCheckedAt || '확인 대기 중'}
            </div>
        </div>
        <button type="button" class="btn-delete">삭제</button>
        `;
        watchlist.appendChild(li);
    });
}

if (registerForm) {
    registerForm.addEventListener('submit', async function (event) {
        event.preventDefault();
        const address = addressInput ? addressInput.value.trim() : '';
        const uniqueNumber = uniqueNumberInput ? uniqueNumberInput.value.trim() : '';
        
        if (!address || !uniqueNumber) {
            showToast('⚠️ 주소와 등기 고유번호를 모두 입력해 주세요.');
            return;
        }
        
        try {
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ addressCode: address, uniqueNumber: uniqueNumber })
            });
            if (!response.ok) throw new Error('등록 실패');
            if (addressInput) addressInput.value = '';
            if (uniqueNumberInput) uniqueNumberInput.value = '';
            showToast('🔔 신규 모니터링 매물이 등록되었습니다.');
            fetchWatchlist();
        } catch (error) {
            localWatchlist.push({ id: Date.now(), address, uniqueNumber, status: 'ACTIVE', lastCheckedAt: '방금 전' });
            if (addressInput) addressInput.value = '';
            if (uniqueNumberInput) uniqueNumberInput.value = '';
            showToast('🔔 신규 모니터링 매물이 등록되었습니다.');
            renderWatchlist(localWatchlist);
        }
    });
}

if (watchlist) {
    watchlist.addEventListener('click', async function (event) {
        if (event.target.classList.contains('btn-delete')) {
            const itemElement = event.target.closest('.watchlist-item');
            const id = Number(itemElement.getAttribute('data-id'));
            
            if (!id || !confirm('정말 이 매물의 모니터링을 해제하시겠습니까?')) return;
            
            try {
                const response = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE' });
                if (!response.ok) throw new Error('삭제 실패');
                showToast('🗑️ 모니터링 매물이 삭제되었습니다.');
                fetchWatchlist();
            } catch (error) {
                localWatchlist = localWatchlist.filter(item => item.id !== id);
                showToast('🗑️ 모니터링 매물이 삭제되었습니다.');
                renderWatchlist(localWatchlist);
            }
        }
    });
}

// ==========================================
// 5. 등기 변동 알림 내역 조회
// ==========================================
const notificationHistoryList = document.getElementById('notificationHistoryList');

let localNotificationHistory = [
    { id: 101, address: '서울시 강남구 역삼동 123-45', changeType: '근저당권 설정', description: '채권최고액 1억 2,000만 원 설정 (OO은행)', createdAt: '2026-09-03 14:30', riskLevel: 'HIGH' },
    { id: 102, address: '서울시 마포구 서교동 456-78', changeType: '소유권 이전', description: '소유자 변경 완료', createdAt: '2026-09-01 10:15', riskLevel: 'MEDIUM' }
];

async function fetchNotificationHistory() {
    try {
        const response = await fetch(NOTIFICATION_API_URL);
        if (!response.ok) throw new Error('조회 실패');
        const data = await response.json();
        renderNotificationHistory(data);
    } catch (error) {
        renderNotificationHistory(localNotificationHistory);
    }
}

function renderNotificationHistory(historyItems) {
    if (!notificationHistoryList) return;
    notificationHistoryList.innerHTML = '';

    if (!historyItems || historyItems.length === 0) {
        notificationHistoryList.innerHTML = `<li style="text-align: center; padding: 20px; color: #A0AEC0; font-size: 13px;">최근 변동 내역이 없습니다.</li>`;
        return;
    }

    historyItems.forEach(item => {
        const li = document.createElement('li');
        let badgeBg = '#E2E8F0', badgeColor = '#4A5568', riskText = '일반 변동';

        if (item.riskLevel === 'HIGH') {
            badgeBg = '#FED7D7'; badgeColor = '#C53030'; riskText = '⚠️ 위험 변동';
        } else if (item.riskLevel === 'MEDIUM') {
            badgeBg = '#FEEBC8'; badgeColor = '#C17D11'; riskText = '⚡ 주의 변동';
        }

        li.style.cssText = 'padding: 12px; background: #F7FAFC; border: 1px solid #E2E8F0; border-radius: 8px; font-size: 13px;';
        li.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
            <strong style="color: #2D3748; font-size: 14px;">${item.address}</strong>
            <span style="background: ${badgeBg}; color: ${badgeColor}; padding: 2px 8px; border-radius: 12px; font-weight: bold; font-size: 11px;">${riskText}</span>
        </div>
        <div style="color: #3182CE; font-weight: bold; margin-bottom: 4px;">📌 [${item.changeType}] ${item.description}</div>
        <div style="color: #A0AEC0; font-size: 11px;">일시: ${item.createdAt}</div>
        `;
        notificationHistoryList.appendChild(li);
    });
}

// ==========================================
// 6. 탭 메뉴 전환
// ==========================================
const tabButtons = document.querySelectorAll('.tab-btn');
const tabContents = document.querySelectorAll('.tab-content');

tabButtons.forEach(button => {
    button.addEventListener('click', () => {
        const targetTab = button.getAttribute('data-tab');
        tabButtons.forEach(btn => btn.classList.remove('active'));
        tabContents.forEach(content => content.classList.remove('active'));
        button.classList.add('active');
        const targetEl = document.getElementById(targetTab);
        if (targetEl) targetEl.classList.add('active');
    });
});

// ==========================================
// 7. [탭 1] 주거비 절약 진단, Chart.js & 지도
// ==========================================
const diagnosisForm = document.getElementById('diagnosisForm');
const diagnosisResultCard = document.getElementById('diagnosisResultCard');
const diagnosisResultContent = document.getElementById('diagnosisResultContent');

if (diagnosisForm) {
    diagnosisForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const monthlyRent = Number(document.getElementById('monthlyRent').value) || 0;
        const location = document.getElementById('location').value;
        
        const mockSavings = Math.min(Math.floor(monthlyRent * 0.4), 25);
        const estimatedSavings = mockSavings > 0 ? mockSavings : 15;

        const mockResult = {
            originalRent: monthlyRent,
            estimatedSavings: estimatedSavings,
            reducedRent: Math.max(monthlyRent - estimatedSavings, 0),
            policies: [
                '청년월세 특별지원 (월 최대 20만원 지원)',
                '청년버팀목 전세자금대출 (연 1.5%~ 저리 대출)',
                `${location} 인근 청년 주거비 지원 사업`
            ]
        };
        renderDiagnosisResult(mockResult);
        showToast('💡 주거비 절약 진단 리포트가 생성되었습니다.');
    });
}

function renderDiagnosisResult(result) {
    diagnosisResultContent.innerHTML = `
    <div style="margin-bottom: 16px; padding: 16px; background: #EBF8FF; border-radius: 8px; border-left: 4px solid #3182CE;">
        <p style="font-size: 14px; color: #2B6CB0; margin-bottom: 4px; font-weight: bold;">매월 아낄 수 있는 예상 주거비</p>
        <p style="font-size: 24px; font-weight: bold; color: #2B6CB0; margin: 0;">약 ${result.estimatedSavings}만 원 절감 가능!</p>
    </div>
    <div>
        <strong style="font-size: 15px; color: #2D3748;">💡 추천 맞춤 정책 및 혜택</strong>
        <ul style="margin-top: 8px; padding-left: 20px; color: #4A5568; line-height: 1.8;">
            ${result.policies.map(policy => `<li>${policy}</li>`).join('')}
        </ul>
    </div>
    `;
    diagnosisResultCard.style.display = 'block';
    diagnosisResultCard.scrollIntoView({ behavior: 'smooth' });

    // Chart.js 시각화 차트 렌더링 호출
    renderSavingsChart(result.originalRent, result.reducedRent);

    setTimeout(() => renderKakaoMap(37.4979, 127.0276), 100);
}

// Chart.js 막대 그래프 렌더링 함수
function renderSavingsChart(originalRent, reducedRent) {
    const ctx = document.getElementById('savingsChart');
    if (!ctx || typeof Chart === 'undefined') return;

    // 기존 차트가 존재하는 경우 파괴(Destroy) 후 재생성하여 중복 렌더링 방지
    if (savingsChartInstance) {
        savingsChartInstance.destroy();
    }

    savingsChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['기존 예상 월세', '지원 적용 후 월세'],
            datasets: [{
                label: '월 지출 금액 (만원)',
                data: [originalRent, reducedRent],
                backgroundColor: [
                    'rgba(226, 232, 240, 0.85)', // 기존 월세 (회색)
                    'rgba(49, 130, 206, 0.85)'   // 절감 후 월세 (파란색)
                ],
                borderColor: [
                    'CBD5E0',
                    '2B6CB0'
                ],
                borderWidth: 1,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return ` ${context.raw}만 원`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return value + '만 원';
                        }
                    }
                }
            }
        }
    });
}

function renderKakaoMap(lat = 37.4979, lng = 127.0276) {
    const container = document.getElementById('map');
    if (!container || typeof kakao === 'undefined' || !kakao.maps) return;

    const options = { center: new kakao.maps.LatLng(lat, lng), level: 4 };
    const map = new kakao.maps.Map(container, options);
    const marker = new kakao.maps.Marker({ position: new kakao.maps.LatLng(lat, lng) });
    marker.setMap(map);
}

// ==========================================
// 8. [탭 2] 등기부 PDF 업로드
// ==========================================
const reportUploadForm = document.getElementById('reportUploadForm');
const reportResultCard = document.getElementById('reportResultCard');
const reportResultContent = document.getElementById('reportResultContent');

if (reportUploadForm) {
    reportUploadForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const pdfInput = document.getElementById('pdfFile');
        if (!pdfInput.files || pdfInput.files.length === 0) {
            showToast('⚠️ PDF 파일을 첨부해 주세요.');
            return;
        }
        
        const mockReport = {
            fileName: pdfInput.files[0].name,
            gradeText: '주의 (융자 비율 확인 필요)',
            badgeColor: '#DD6B20',
            badgeBg: '#FEEBC8',
            mortgageAmount: '1억 2,000만 원',
            debtRatio: '65%',
            hasSeizure: '없음 (가압류/가처분 내역 없음)',
            summary: '선순위 근저당권이 설정되어 있습니다. 매매가 대비 융자 비율을 반드시 확인하시기 바랍니다.'
        };
        renderReportResult(mockReport);
        showToast('🛡️ 등기부등본 위험도 분석이 완료되었습니다.');
    });
}

function renderReportResult(report) {
    reportResultContent.innerHTML = `
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 12px 16px; background: ${report.badgeBg}; border-radius: 8px;">
        <span style="font-size: 14px; font-weight: bold; color: ${report.badgeColor};">분석 파일: ${report.fileName}</span>
        <span style="padding: 4px 12px; border-radius: 20px; background: ${report.badgeColor}; color: white; font-weight: bold; font-size: 13px;">${report.gradeText}</span>
    </div>
    <div style="display: grid; gap: 12px; margin-bottom: 16px; color: #2D3748; font-size: 14px;">
        <div style="padding: 10px; background: white; border-radius: 6px; border: 1px solid #E2E8F0;"><strong>📌 선순위 근저당:</strong> ${report.mortgageAmount}</div>
        <div style="padding: 10px; background: white; border-radius: 6px; border: 1px solid #E2E8F0;"><strong>📊 융자 비율:</strong> ${report.debtRatio}</div>
        <div style="padding: 10px; background: white; border-radius: 6px; border: 1px solid #E2E8F0;"><strong>⚖️ 권리 침해(가압류 등):</strong> ${report.hasSeizure}</div>
    </div>
    <div style="padding: 12px; background: #EDF2F7; border-radius: 6px; color: #4A5568; font-size: 13px;"><strong>💡 위험 요약:</strong> ${report.summary}</div>
    `;
    reportResultCard.style.display = 'block';
    reportResultCard.scrollIntoView({ behavior: 'smooth' });
}

// ==========================================
// 9. [탭 4] 인증 모달 및 회원 관리
// ==========================================
let isSignUpMode = false;
let currentUser = null;

function initAuthEvents() {
    const openLoginModalBtn = document.getElementById('openLoginModalBtn');
    const closeLoginModalBtn = document.getElementById('closeLoginModalBtn');
    const loginModal = document.getElementById('loginModal');
    const emailAuthForm = document.getElementById('emailAuthForm');
    const toggleAuthModeBtn = document.getElementById('toggleAuthModeBtn');
    const logoutBtn = document.getElementById('logoutBtn');

    if (openLoginModalBtn) openLoginModalBtn.addEventListener('click', () => { resetAuthForm(); loginModal.style.display = 'flex'; });
    if (closeLoginModalBtn) closeLoginModalBtn.addEventListener('click', () => { loginModal.style.display = 'none'; });

    if (toggleAuthModeBtn) {
        toggleAuthModeBtn.addEventListener('click', () => {
            isSignUpMode = !isSignUpMode;
            document.getElementById('modalTitle').innerText = isSignUpMode ? '🏠 SafeHouse 회원가입' : '🏠 SafeHouse 시작하기';
            document.getElementById('nameGroup').style.display = isSignUpMode ? 'block' : 'none';
            document.getElementById('emailAuthBtn').innerText = isSignUpMode ? '회원가입 완료' : '이메일로 로그인';
            document.getElementById('toggleAuthModeBtn').innerText = isSignUpMode ? '로그인' : '회원가입';
        });
    }

    if (emailAuthForm) {
        emailAuthForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const email = document.getElementById('authEmail').value.trim();
            currentUser = { name: `${email.split('@')[0]} 님`, email: email, provider: 'email' };
            updateMyPageUI();
            loginModal.style.display = 'none';
            showToast('로그인되었습니다.');
        });
    }

    document.getElementById('kakaoLoginBtn')?.addEventListener('click', () => {
        currentUser = { name: '카카오 사용자 님', email: 'kakao@safehouse.com', provider: 'kakao' };
        updateMyPageUI();
        loginModal.style.display = 'none';
        showToast('💬 카카오 계정으로 로그인되었습니다.');
    });

    if (logoutBtn) logoutBtn.addEventListener('click', () => { currentUser = null; updateMyPageUI(); showToast('로그아웃되었습니다.'); });
}

function updateMyPageUI() {
    const loginPrompt = document.getElementById('loginPrompt');
    const userInfoSection = document.getElementById('userInfoSection');

    if (currentUser) {
        if (loginPrompt) loginPrompt.style.display = 'none';
        if (userInfoSection) userInfoSection.style.display = 'block';
        document.getElementById('userNickname').innerText = currentUser.name;
        document.getElementById('userEmail').innerText = currentUser.email;
    } else {
        if (loginPrompt) loginPrompt.style.display = 'block';
        if (userInfoSection) userInfoSection.style.display = 'none';
    }
}

function resetAuthForm() {
    document.getElementById('emailAuthForm')?.reset();
    isSignUpMode = false;
}

// ==========================================
// 10. 포트원 결제 처리 로직
// ==========================================
function initPaymentEvents() {
    const openPaymentModalBtn = document.getElementById('openPaymentModalBtn');
    const closePaymentModalBtn = document.getElementById('closePaymentModalBtn');
    const paymentModal = document.getElementById('paymentModal');
    const payKakaoBtn = document.getElementById('payKakaoBtn');
    const payCardBtn = document.getElementById('payCardBtn');

    if (openPaymentModalBtn) {
        openPaymentModalBtn.addEventListener('click', () => {
            if (!currentUser) {
                showToast('⚠️ 결제를 진행하시려면 먼저 로그인해 주세요.');
                document.getElementById('loginModal').style.display = 'flex';
                return;
            }
            paymentModal.style.display = 'flex';
        });
    }

    if (closePaymentModalBtn) {
        closePaymentModalBtn.addEventListener('click', () => {
            paymentModal.style.display = 'none';
        });
    }

    // 카카오페이 결제 버튼
    if (payKakaoBtn) {
        payKakaoBtn.addEventListener('click', () => {
            requestPayment('kakaopay');
        });
    }

    // 일반 카드 결제 버튼
    if (payCardBtn) {
        payCardBtn.addEventListener('click', () => {
            requestPayment('html5_inicis');
        });
    }
}

// 포트원 결제창 호출 함수
function requestPayment(pgProvider) {
    if (!window.IMP) {
        showToast('⚠️ 결제 모듈이 로드되지 않았습니다.');
        return;
    }

    const merchantUid = `mid_${new Date().getTime()}`; // 주문 고유번호

    // window.IMP를 사용하여 안전하게 결제창 호출
    window.IMP.request_pay({
        pg: pgProvider,                       // PG사 (kakaopay / html5_inicis)
        pay_method: "card",                  // 결제 방식
        merchant_uid: merchantUid,           // 주문번호
        name: "SafeHouse 프리미엄 월간 구독", // 상품명
        amount: 9900,                        // 결제 금액
        buyer_email: currentUser?.email || "user@safehouse.com",
        buyer_name: currentUser?.name || "사용자",
        buyer_tel: "010-1234-5678"
    }, async function (rsp) {
        if (rsp.success) {
            // 결제 성공 시 백엔드로 결제 검증 요청
            try {
                const verifyRes = await fetch(PAYMENT_VERIFY_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        impUid: rsp.imp_uid,
                        merchantUid: rsp.merchant_uid,
                        amount: rsp.paid_amount
                    })
                });

                document.getElementById('paymentModal').style.display = 'none';
                showToast('🎉 프리미엄 멤버십 결제가 완료되었습니다!');
            } catch (error) {
                document.getElementById('paymentModal').style.display = 'none';
                showToast('🎉 (테스트) 결제가 완료되었습니다!');
            }
        } else {
            showToast(`❌ 결제 실패: ${rsp.error_msg}`);
        }
    });
}