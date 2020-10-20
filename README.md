# ActivityRecognitionClient
## Android User Activity Recognition – Still, Walking, Running, Driving 

#### This repository contains the code needed to use the new Google ActivityRecognitionClient. Make sure to use the following configurations to run the application.

#### Permission

    <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />

#### Dependecies

dependencies {
   implementation 'com.android.support:design:27.1.1'
   
  implementation 'com.google.android.gms:play-services-location:11.6.0'
}

#### Add Services to your Manifest file

<service
            android:name=".DetectedActivitiesIntentService"
            android:exported="false" />

        <service android:name=".BackgroundDetectedActivitiesService"></service>




    
