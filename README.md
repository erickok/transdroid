Transdroid
==========

[www.transdroid.org](https://www.transdroid.org/)
[Twitter](https://twitter.com/transdroid) - [transdroid@2312.nl](mailto:transdroid@2312.nl)

Manage torrents from your Android device.

<a href="https://transdroid.org/latest">
    <img src="https://transdroid.org/images/getontransdroid.png"
    alt="Get it on transdroid.org"
    height="80">
</a>
<a href="https://f-droid.org/packages/org.transdroid.full/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">
</a>
<a href="https://play.google.com/store/apps/details?id=org.transdroid.lite">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
    alt="Get it on Google Play"
    height="80">
</a>

<img src="https://2312.nl/images/screenshot_transdroid_main.png" alt="Screen shot of the main torrents listing screen" width="280" />


Manage your torrents from your Android device with Transdroid.
All popular clients are supported: µTorrent, Transmission, rTorrent, Vuze, Deluge, BitTorrent 6, qBittorrent, and many more.
You can view and manage running torrents and individual files.
Adding is easy via the integrated search or RSS feeds (full version required).
Monitor progress using the home screen widget or background alarm service.

Contributions
=============

Code and design contributions are very welcome.
You might want to contact me via social networks (Twitter) or e-mail first.
Please note that all code will be licensed in GNU GPLv3.

Please respect the coding standards for easier merging.
`master` contains the current release version of Transdroid while `dev` contains the active development version.
However, larger and new features will be developed in their own branch.

Code structure
==============

Starting with version 2.3.0, Transdroid is developed in Android Studio, fully integrating with the Gradle build system.
It is (since version 2.5.21) compiled against Android 12 (API level 31) and (since version 2.2.0) supporting Android ICS (API level 15) and up only.
To support lite (Transdrone, specially for the Play Store) and full (Transdroid) versions of the app, build flavours are defined in gradle, which contain version-specific resources.
Dependencies are managed via JCentral et al. in the app's build.gradle file.

Developed By
============

Designed and developed by [Eric Kok](mailto:eric@2312.nl) of [2312 development](https://2312.nl/).
Contributions by various others (see commit log).

License
=======

    Copyright 2010-2022 Eric Kok et al.

    Transdroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Transdroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Transdroid.  If not, see <https://www.gnu.org/licenses/>.

Some code/libraries/resources are used in the project:
*  [Android Jetpack (AndroidX)](https://developer.android.com/jetpack)
    The Android Open Source Project
    Apache License, Version 2.0
*  [AndroidAnnotations](http://androidannotations.org/)
    Pierre-Yves Ricau (eBusinessInformations) et al.
    Apache License, Version 2.0
*  [ORMLite](https://github.com/j256/ormlite-core) and [ORMLite Android](https://github.com/j256/ormlite-android)
    Gray Watson
    ISC License
*  [Android Universal Image Loader](https://github.com/nostra13/Android-Universal-Image-Loader)
    Sergey Tarasevich
    Apache License, Version 2.0
*  [FloatingActionButton](https://github.com/zendesk/android-floating-action-button)
    Oleksandr Melnykov, Zendesk
    Apache License, Version 2.0
*  [Snackbar](https://github.com/nispok/snackbar)
    William Mora
    MIT License
*  [Java implementation of Rencode](https://github.com/aegnor/rencode-java)
    Daniel Dimovski
    MIT License
*  [OpenJPA's Base16Encoder](https://github.com/apache/openjpa)
    Marc Prud'hommeaux
    Apache OpenJPA
*  [Base64](http://iharder.sourceforge.net/current/java/base64/)
    Robert Harder
    Public Domain
*  [aXMLRPC](https://github.com/gturri/aXMLRPC)
    Tim Roes
    MIT License
*  [Material Dialogs](https://github.com/afollestad/material-dialogs)
    Aidan Follestad
    Apache License, Version 2.0
*  [Android-Job](https://github.com/evernote/android-job)
    Evernote Corporation
    Apache License, Version 2.0
*  [android-ColorPickerPreference](https://github.com/attenzione/android-ColorPickerPreference)
    Daniel Nilsson and Sergey Margaritov
    Apache License, Version 2.0
*  RssParser ([learning-android](https://github.com/tanepiper/learning-android))
    Tane Piper
    Public Domain
*  [Funnel icon](https://thenounproject.com/term/funnel/5608/)
    Naomi Atkinson from The Noun Project
    Creative Commons Attribution 3.0
