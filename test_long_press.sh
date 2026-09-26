adb -s 192.168.50.247:5555 logcat -c
echo "Waiting for key events..."
adb -s 192.168.50.247:5555 logcat -s KeyInterceptorService
