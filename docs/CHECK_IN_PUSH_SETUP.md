# Android check-in notifications

Android uses Firebase Cloud Messaging; APNs is only for Apple devices. The backend
already accepts `PUT /api/account/push-devices/{installationId}` with the bearer
token and fields `platform`, `environment`, `token` and `app_version`, and can
remove the installation with `DELETE` at the same path.

To enable the Android transport:

1. Create or select the Firebase project used by the backend, register the Android
   application `io.codepassion.wildforce.android`, and put its downloaded
   `google-services.json` in `app/`. The file contains project configuration,
   not the server service-account private key.
2. Enable the Firebase Cloud Messaging API for that project and configure the
   backend's `FIREBASE_CREDENTIALS` with its service-account credentials. Keep private keys
   on the server; never include them in the app.
3. Run the backend queue worker for the `notifications` queue. Rebuild Android
   after the configuration is present. The build enables Google Services and
   Firebase Messaging only when the file exists; without it, push is disabled.
4. On an Android device with Google Play services, grant notification permission,
   sign in, verify that the installation appears on the backend, and test new,
   reminder and reviewed check-ins in foreground and background. Test logout,
   login to another account, and token rotation as well.

The token registration code retries when the app resumes or the account changes.
Logout requests device removal, but a failed offline removal is not yet retried
after process exit; this and real FCM delivery need end-to-end verification before
production release.
