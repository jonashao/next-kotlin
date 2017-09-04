# next-kotlin
Kotlin version of [Next music player](https://github.com/jonashao/next)

*Welcome to join and contribute.*

This is an extremely simple music player, only letting user to swipe to skip , 
or pause and start playing music.
And, It super saves power, compared to wechat and facebook, its power consumption can be ignored.

It might be a good example for learning Kotlin, android music player,
mvp framework, dagger and Realm, since it's simple and clean.

<img src="https://cloud.githubusercontent.com/assets/7600440/26617744/7046dd06-4609-11e7-83ad-29f70e5ca359.jpg" width="300" height="486"/> <img src="https://cloud.githubusercontent.com/assets/7600440/26617745/704da2f8-4609-11e7-8084-3c1b4704610a.jpg" width="300" height="486"/>

## Pace adaptation

Get Sensor: [**TYPE_STEP_DETECTOR**](https://developer.android.com/reference/android/hardware/Sensor.html#TYPE_STEP_DETECTOR) timestamps.

when user enter this pace adaptation mode, start to count the timestamps.

calculating foot frequency every 10 seconds or 30 steps.







## Libs 

- Realm
- RxJava
- Dagger2

## Todo
- [ ] fetching album arts from Last.fm
- [ ] recording playing behaivours to recommend music
