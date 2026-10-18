# 🎮 Arcade Hub

A multi-game Android arcade app built with **Kotlin** and **Jetpack Compose**. Sign up, play 15 games, beat the bots, challenge friends online, and climb the leaderboards.

## ✨ Features
- **Login & signup** with Firebase Authentication
- **15 games** in one app with a playful, flat casual-game UI
- **Bots with 3 difficulty levels** (Easy / Medium / Hard)
- **Local multiplayer** for 2–4 players, with bots able to fill any seat
- **Online rooms** using a 4-digit join code, synced live with Firestore
- **Leaderboards**: a global points ranking plus one for every game

## 🕹️ Games
| Type | Games |
|---|---|
| Solo | Flappy Bird, Snake, 2048, Memory Match, Minesweeper, Breakout, Whack-a-Mole |
| vs Bot / Pass & Play | Tic Tac Toe, Connect Four, Battleship, Rock Paper Scissors |
| 2–4 Players | Mini Golf Battle, Dots & Boxes, Ludo Race |
| Online | Tic Tac Toe and Connect Four rooms |

## 🤖 Bot AI
- **Tic Tac Toe / Connect Four:** minimax with alpha-beta pruning on Hard
- **Battleship:** random shots on Easy, hunt-and-target on Medium, probability-density targeting on Hard
- **Mini Golf:** bots simulate candidate shots with the real physics and pick the best
- **Rock Paper Scissors:** frequency and pattern prediction on Hard

## 🏆 Leaderboards
- **Global:** total points from all games
  - +5 per finished solo game
  - +5 / +10 / +20 for beating an Easy / Medium / Hard bot
  - +15 per online win
- **Per game:** best score for solo games, total wins for bot and multiplayer games

## 🛠️ Tech Stack
- Kotlin, Jetpack Compose, Compose Canvas (game loops and rendering)
- Navigation Compose, MVVM
- Firebase Authentication, Cloud Firestore (leaderboards and real-time rooms)

## 🚀 Setup
1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and add an Android app with package `com.example.arcadehub`.
3. Download `google-services.json` and place it in `app/`.
4. Enable **Email/Password** sign-in under Authentication.
5. Create a Firestore database with the ID **(default)** and publish the security rules from `SETUP.md`.
6. Run the app.

## 📸 Screenshots
_Add screenshots here._

## 📄 License
MIT
