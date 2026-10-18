# Arcade Hub - FULL project setup

## 1. Create the project
Android Studio > New Project > **Empty Activity** (Compose).
- Name: Arcade Hub | Package: **com.example.arcadehub** | Min SDK: API 26

## 2. Firebase
1. console.firebase.google.com > Add project > add Android app (package above).
2. Put **google-services.json** in the `app/` folder.
3. Authentication > enable **Email/Password**.
4. Firestore Database > Create database, then paste these rules (Rules tab > Publish):

    rules_version = '2';
    service cloud.firestore {
      match /databases/{db}/documents {
        match /users/{uid} {
          allow read: if request.auth != null;
          allow write: if request.auth != null && request.auth.uid == uid;
        }
        match /leaderboards/{game}/entries/{uid} {
          allow read: if request.auth != null;
          allow write: if request.auth != null && request.auth.uid == uid;
        }
        match /rooms/{room} {
          allow read, write: if request.auth != null;
        }
      }
    }

## 3. Gradle
Project-level build.gradle.kts, inside plugins { }:
    id("com.google.gms.google-services") version "4.4.2" apply false

app/build.gradle.kts, inside plugins { }:
    id("com.google.gms.google-services")

app/build.gradle.kts, inside dependencies { }:
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")

Sync Now.

## 4. Manifest
Above <application>:
    <uses-permission android:name="android.permission.INTERNET" />

## 5. Copy files
- Copy everything in `app/src/main/` from this zip into your project's `app/src/main/` (overwrite).
- Delete the template's ui/theme/Color.kt, Theme.kt, Type.kt and its default MainActivity.kt.
- Delete the template's ic_launcher*.webp files, res/drawable/ic_launcher_foreground.xml and ic_launcher_background.xml.
- Run.

## What is inside
Games (15): Flappy Bird, Snake, 2048, Memory Match, Minesweeper, Breakout, Whack-a-Mole (solo, score leaderboards);
Tic Tac Toe, Connect Four, Battleship, Rock Paper Scissors (vs bot Easy/Medium/Hard or pass & play);
Mini Golf Battle, Dots & Boxes, Ludo Race (2-4 players, any seat can be a bot);
Online Rooms (Tic Tac Toe and Connect Four with a 4-digit code).
Leaderboards: global points + one per game.

## Notes
- Scores are written from the client, so a determined user could cheat. For a real release, move score writes to a Cloud Function.
- In pass & play and bot modes, Player 1 (you) is the account that gets the win credit.
