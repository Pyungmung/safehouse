// firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js');

// 1단계에서 발급받은 firebaseConfig 입력
const firebaseConfig = {
    apiKey: "AIzaSyB8SZ2dQTOfyOt8xxugkSu8Oew3DmZRUFY",
    authDomain: "safehose-61162.firebaseapp.com",
    projectId: "safehose-61162",
    storageBucket: "safehose-61162.firebasestorage.app",
    messagingSenderId: "985908130594",
    appId: "1:985908130594:web:58447b3314daeba0b7eb7b",
    measurementId: "G-X9GME64SW4"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// 백그라운드 수신 시 푸시 알림 팝업 노출
messaging.onBackgroundMessage((payload) => {
    console.log('[SW] 백그라운드 메시지 수신:', payload);
    
    const notificationTitle = payload.notification?.title || '등기부 변동 알림';
    const notificationOptions = {
        body: payload.notification?.body || '새로운 등기 변동 내역이 감지되었습니다.',
        icon: '/icon.png'
    };
    
    self.registration.showNotification(notificationTitle, notificationOptions);
});