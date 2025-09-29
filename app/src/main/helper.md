voice commands are heard and interpreted, but the command is not reflected to switch the devices, also the search is not returning any device even if they exist, under settings, most don't  work, they crash the app

Process: com.example.homie, PID: 12888
java.lang.RuntimeException: Unable to start activity ComponentInfo{com.example.homie/com.example.homie.ui.settings.PrivacySecuritySettingsActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.Switch.setChecked(boolean)' on a null object reference
Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.Switch.setChecked(boolean)' on a null object reference
	at com.example.homie.ui.settings.PrivacySecuritySettingsActivity.loadSettings(PrivacySecuritySettingsActivity.java:51)
	at com.example.homie.ui.settings.PrivacySecuritySettingsActivity.onCreate(PrivacySecuritySettingsActivity.java:37)

E  [com.example.homie/com.example.homie.ui.settings.HelpSupportActivity#0](this:0xb400007cdfcf88e0,id:-1,api:0,p:-1,c:-1) id info cannot be read from 'com.example.homie/com.example.homie.ui.settings.HelpSupportActivity#0'
Process: com.example.homie, PID: 13046
java.lang.RuntimeException: Unable to start activity ComponentInfo{com.example.homie/com.example.homie.ui.settings.AccountSettingsActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.EditText.setText(java.lang.CharSequence)' on a null object reference
Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.EditText.setText(java.lang.CharSequence)' on a null object reference
	at com.example.homie.ui.settings.AccountSettingsActivity.loadUserData(AccountSettingsActivity.java:44)
	at com.example.homie.ui.settings.AccountSettingsActivity.onCreate(AccountSettingsActivity.java:30)


Process: com.example.homie, PID: 13249
java.lang.RuntimeException: Unable to start activity ComponentInfo{com.example.homie/com.example.homie.ui.settings.NotificationsSettingsActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.Switch.setChecked(boolean)' on a null object reference
Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.Switch.setChecked(boolean)' on a null object reference
	at com.example.homie.ui.settings.NotificationsSettingsActivity.loadSettings(NotificationsSettingsActivity.java:51)
	at com.example.homie.ui.settings.NotificationsSettingsActivity.onCreate(NotificationsSettingsActivity.java:37)

also under room details, clicking on a device card brings a section under development, kindly work on the section to fully implement it