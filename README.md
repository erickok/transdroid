Transdroid
==========

[www.transdroid.org](http://www.transdroid.org)
[Google+](https://plus.google.com/u/0/b/106240944422491053650/106240944422491053650) - [Twitter](https://twitter.com/transdroid) - [Old Google Code site](https://code.google.com/p/transdroid/) - [transdroid@2312.nl](transdroid@2312.nl)
"Manage your torrents from your Android device"

![Screen shot of the main torretns listing screen](http://2312.nl/img/portfolio-transdroid/240x400-transdroid-main.png)

Manage your torrents from your Android device with Transdroid. All popular clients are supported, including µTorrent, Transmission, rTorrent, Vuze, Deluge and BitTorrent 6. You can view and manage the running torrents and individual files. Adding is easy via the integrated search, RSS feeds or the barcode scanner. Monitor progress using the home screen widget or background alarm service.

Transdroid was migrated from its Google Code Mercurial repo to GitHub with the 2.0-alpha3 tag and its .apk release.

Contributions
=============

Code and design contributions are very welcome. You might want to contact me via social networks (G+, Twitter) or e-mail first. Please note all code will be GNU GPL v3 licensed.

Please respect the coding standards for easier merging. master contains the current release version of Transdroid while dev contains the active development version. However, larger, new features are developed in their own branch.

Code structure
==============

Transdroid is currently developed in Eclipse, against Android 4.4 (API level 19) and since version 2.2.0 supporting ICS (API level 15) and up. To support lite and full version of the app, the core UI is contained in a separate Android library project. The torrent client adapters are also in a seperate project, called Transdroid Connect, but this is a classic Java project. If the Connect project is updated, a fresh .jar needs to be exported to core/libs/transdroid.jar. In order to do so, run ant (without parameters) within the lib/ directory.

* core - Contains the core Android library project with UI, services, resources, etc.
* full - Full app version (website release) through AndroidManifest.xml configuration
* lite - Lite version (for Play Store, without search and RSS) through AndroidManifest.xml configuration
* external - Local copies of required external Android libraries
* lib - The Torrent Connect project containing torrent client communication code

Developed By
============

Designed and developed by [Eric Kok](eric@2312.nl) of [2312 development](http://2312.nl). Contributions by various others (see commit log).

License
=======

    Copyright 2010-2014 Eric Kok et al.

    Transdroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Transdroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.

Some code/libraries/resources are used in the project:

*  [AndroidAnnotations](http://androidannotations.org/)
    Pierre-Yves Ricau (eBusinessInformations) et al.
    Apache License, Version 2.0
*  [ActionBar-PullToRefresh](https://github.com/chrisbanes/ActionBar-PullToRefresh)
    Chris Banes
    Apache License, Version 2.0
*  [Crouton](https://github.com/keyboardsurfer/Crouton)
    Code: Benjamin Weiss (Neofonie Mobile Gmbh) et al.
    Idea: Cyril Mottier
    Apache License, Version 2.0
*  [Base16Encoder](http://openjpa.apache.org/)
    Marc Prud'hommeaux
    Apache OpenJPA
*  MultipartEntity
    Apache Software Foundation
    Apache License, Version 2.0
*  RssParser ([learning-android](http://github.com/digitalspaghetti/learning-android))
    Tane Piper
    Public Domain
*  [Base64](http://iharder.net/base64)
    Robert Harder
    Public Domain
*  [aXMLRPC](https://github.com/timroes/aXMLRPC)
    Tim Roes
    MIT License
*  [android-ColorPickerPreference](https://github.com/attenzione/android-ColorPickerPreference)
    Daniel Nilsson and Sergey Margaritov
    Apache License, Version 2.0
*  [Funnel icon](http://thenounproject.com/noun/funnel/#icon-No5608)
    Naomi Atkinson from The Noun Project
    Creative Commons Attribution 3.0

