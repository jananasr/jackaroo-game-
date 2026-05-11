# 🎲 Jackaroo Board Game (Java + JavaFX)

## 📌 Overview

This project is a **custom implementation of the Jackaroo board game**, developed as part of the *Computer Programming Lab (CSEN 401)* course at the German University in Cairo.

The game is designed as a **single-player experience against 3 CPU opponents**, featuring a fully functional game engine, custom rules, and an interactive JavaFX graphical user interface.

---

## 🧠 Key Concepts

* Object-Oriented Programming (OOP)
* Game Engine Design
* MVC Architecture (Model–View–Controller)
* Event-Driven Programming (JavaFX)
* Exception Handling & Validation
* File Handling (CSV-based card system)

---

## 🎮 Game Features

* 🧩 Full implementation of **custom Jackaroo rules**
* 🤖 Single player vs **3 CPU players**
* 🔁 Turn-based gameplay with strategic decision-making
* 🎴 Card-driven mechanics (movement, swap, burn, save, etc.)
* 🎯 Goal: Move all marbles from Home Zone to Safe Zone
* ⚠️ Trap cells and special zones (Base, Safe, Entry)
* 🔄 Dynamic rounds with card distribution and discard system

---

## 🏗️ System Architecture

### 🔹 Engine Layer

* Game logic and rules enforcement
* Turn handling and round management
* Player and CPU behavior

### 🔹 Model Layer

* Cards (Standard & Wild)
* Players and Marbles
* Board structure (Cells, Safe Zones)

### 🔹 View Layer (JavaFX)

* Interactive game board
* Player panels and card display
* Real-time updates reflecting game state

---

## 🛠️ Technologies Used

* **Java**
* **JavaFX**
* **OOP Principles**
* **CSV File Processing**

---

## 📂 Project Structure

```id="proj1"
src/
│
├── engine/            # Game engine (Game, GameManager)
├── engine/board/      # Board logic (Board, Cell, SafeZone)
├── model/             # Core entities (Player, Marble, Colour)
├── model/card/        # Card hierarchy
│   ├── standard/      # Ace, King, Queen, Jack, etc.
│   └── wild/          # Burner, Saver
├── exception/         # Custom exceptions
├── view/              # JavaFX GUI components
│
resources/
├── Cards.csv
├── application.css
```

---

## ▶️ How to Run

1. Clone the repository:

   ```
   git clone https://github.com/your-username/jackaroo-game.git
   ```
2. Open in IntelliJ / Eclipse
3. Configure JavaFX
4. Run the main GUI class

---

## 📸 Demo Video

Watch the project demo here:
https://drive.google.com/file/d/1ZdGkOWgtfwRWX5q97sIhSpPGQkHvq7AM/view?usp=drivesdk

---

## ⚠️ Notes

* JavaFX must be configured correctly to run the GUI
* The game handles invalid actions using custom exceptions without crashing

---

## 👩‍💻 Author

Developed as part of coursework at the German University in Cairo.
