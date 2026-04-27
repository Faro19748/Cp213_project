<div align="center">

# 🐱 SweepNeko

**เกม Action สไลด์หน้าจอสำหรับ Android — ฟันศัตรูด้วยท่า Slash เพื่อความอยู่รอด!**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-35-orange?style=for-the-badge)

</div>

---

## 📖 เกี่ยวกับโปรเจกต์ (About The Project)

**SweepNeko** คือเกม Action บน Android ที่ผู้เล่นควบคุมตัวละคร "แมว" ผ่านการลากนิ้วบนหน้าจอเพื่อสร้างท่าโจมตี Slash ฟันศัตรูที่บุกเข้ามาโดยรอบ เป้าหมายคือการอยู่รอดให้ได้นานที่สุด ผ่านระบบ Wave ที่ยากขึ้นเรื่อย ๆ พร้อมระบบ Combo, Ultimate และ Power-Up หลากหลายรูปแบบ

โปรเจกต์นี้พัฒนาขึ้นเป็นส่วนหนึ่งของรายวิชา **CP213** โดยใช้ Kotlin และ Jetpack Compose บน MVVM Architecture

---

## ✨ ฟีเจอร์หลัก (Key Features)

### ⚔️ ระบบการต่อสู้ (Combat System)
| ฟีเจอร์ | รายละเอียด |
|---|---|
| **Slash** | ลากนิ้วบนหน้าจอเพื่อสร้างแนวโจมตี ตรวจจับการชนด้วย Line-Rect Intersection |
| **Stamina** | การ Slash ใช้ SP (แถบสีเขียว) และจะฟื้นฟูอัตโนมัติเมื่อหยุดลาก |
| **Combo System** | สะสม Combo จากการโจมตีโดนศัตรูต่อเนื่อง — ทุก 10 Combo ไม้ถัดไปจะกลายเป็น **Red Slash** ที่คืน SP ทันที |
| **Ultimate Gauge** | ชาร์จ Gauge สูงสุด 100% จากการโจมตี เมื่อเต็มสามารถปล่อย **Triple Slash** ด้วยพลังทำลายสูง |

### 👾 ประเภทศัตรู (Enemy Types)
| ประเภท | HP | ความเร็ว | ความสามารถพิเศษ |
|---|---|---|---|
| **Normal** | 1 | ปานกลาง | เดินตรงมาหาผู้เล่น |
| **Fast** | 1 | เร็ว | ขนาดเล็ก เคลื่อนที่รวดเร็ว |
| **Big** | 3 | ช้า | ขนาดใหญ่ ต้องฟัน 3 ครั้ง |
| **Shooting** | 1 | ปานกลาง | หยุดยิง Projectile ใส่ผู้เล่นเมื่ออยู่ในระยะ |
| **Boss** | 10 | ช้ามาก | เกิดทุก 5 Wave พร้อม BGM พิเศษ, ทน Ultimate |

> ศัตรูแต่ละตัวมี Spawn Direction แบบสุ่ม (บน/ซ้าย/ขวา) และมี Hitbox ที่คำนวณแม่นยำแยกต่างหากจาก Sprite

### 🎁 Power-Up System
Power-Up จะปรากฏทุกครั้งที่ Wave เพิ่มขึ้น เคลื่อนที่กระดอนไปมาบนหน้าจอ ผู้เล่นต้องฟันเพื่อเก็บ

| Power-Up | เอฟเฟกต์ |
|---|---|
| 🥫 **Cat Can** | ฟื้น HP +40 |
| 🍫 **Cat Bar** | SP ไม่ลดลง 5 วินาที (แถบสีรุ้ง) |
| ⏱️ **Time Stop** | ศัตรูและ Projectile ช้าลง 87.5% เป็นเวลา 5 วินาที |

### 💣 C4 Hazard
- ระเบิด C4 จะ Spawn ทุก 20 วินาทีและกระดอนไปมาบนหน้าจอ
- ห้ามฟัน! การ Slash โดน C4 จะทำให้ **เสีย HP -30** และเกิดเอฟเฟกต์ระเบิด

### 🌊 Wave System
- เริ่มต้นที่ Wave 1 ต้องฆ่าศัตรู 15 ตัวเพื่อผ่าน Wave
- แต่ละ Wave ที่ผ่านไป เป้าหมายจะเพิ่มขึ้น +5 ตัว
- ความถี่การ Spawn ศัตรูเพิ่มขึ้น และ Boss จะปรากฏทุก 5 Wave

### 🎵 Sound System
- **BGM**: เพลงเมนู / เพลงในเกม / เพลง Boss / เพลง Game Over
- **SFX**: เสียงฟัน, เสียงระเบิด, เสียงโดนโจมตี, เสียง Ultimate
- ปรับระดับเสียง BGM และ SFX แยกกันได้จากหน้า Settings

### 🐾 Easter Egg
- กดตัวละครแมวใน Menu 10 ครั้ง — Sprite จะสลับเป็น **แมวจริง!** 🐱

---

## 🗂️ โครงสร้างโปรเจกต์ (Project Architecture)

โปรเจกต์ใช้รูปแบบ **MVVM (Model-View-ViewModel)** ร่วมกับ **Unidirectional Data Flow** ผ่าน `StateFlow`

```
Cp213_project/
├── Game Assets/                  # ไฟล์ Asset ดิบ (ก่อน import เข้า Android)
│   ├── *.gif / *.png             # Sprite ตัวละครและศัตรู
│   └── ost/                      # ไฟล์เพลงต้นฉบับ
│
└── Project/                      # Android Project (Gradle)
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/sweepneko/
        │   ├── Enemy.kt              # [Model] Data class + EnemyType enum + Spawn logic
        │   ├── GameState.kt          # [Model] State หลักของเกมทั้งหมด (data class)
        │   ├── GameViewModel.kt      # [ViewModel] Game Loop, Logic, Physics, Input Handler
        │   ├── GameScreen.kt         # [View] Compose UI — วาด Sprite, Slash, HUD
        │   ├── GameComponents.kt     # [View] Compose Components — HP Bar, Pause Menu, etc.
        │   ├── MenuGame.kt           # [View] หน้าจอ Menu หลัก + Animation
        │   ├── MainGame.kt           # Activity wrapper สำหรับ GameScreen
        │   └── SoundManager.kt       # [Service] Singleton จัดการเสียง BGM + SFX
        └── res/
            ├── drawable/             # Sprite GIF/PNG ทั้งหมด
            └── raw/                  # ไฟล์เพลง .mp3
```

### แผนผังการทำงาน (Data Flow)

```
User Touch Input
      │
      ▼
 GameScreen.kt  ──── pointerInput ────►  GameViewModel.kt
  (Compose UI)                            (ViewModel)
      │                                       │
      │  collectAsState()                     │  _state: MutableStateFlow<GameState>
      │◄──────────────────────────────────────┤
      │                                       │
      │  Render Sprites/HUD                   │  Game Loop (Coroutine - Dispatchers.Default)
      ▼                                       │   ├─ Enemy movement & collision
 SoundManager.kt ◄──────────────────────────►│   ├─ Projectile physics
  (Singleton)                                │   ├─ Wave management
                                             │   ├─ Power-up spawning
                                             │   └─ Slash hit detection
```

### ไฟล์สำคัญ

| ไฟล์ | บทบาท |
|---|---|
| `GameState.kt` | เก็บ State ของเกมทั้งหมดใน data class เดียว (HP, Enemies, Wave, etc.) |
| `GameViewModel.kt` | รัน Game Loop บน Background Coroutine ≈ 60 FPS, คำนวณ Physics + Collision |
| `GameScreen.kt` | Subscribe `StateFlow` และวาดผลลัพธ์บน Compose Canvas/Image |
| `Enemy.kt` | กำหนดพฤติกรรม, Stats และ Spawn Logic ของศัตรูทุกประเภท |
| `SoundManager.kt` | Singleton จัดการ MediaPlayer (BGM) + SoundPool (SFX) พร้อม Volume Control |

---

## 🛠️ Tech Stack

| ส่วน | เทคโนโลยี |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Image Loading | Coil 2.6.0 (รองรับ GIF) |
| Audio | MediaPlayer (BGM) + SoundPool (SFX) |
| Concurrency | Kotlin Coroutines |
| Storage | SharedPreferences (High Score) |
| Min SDK | 35 (Android 15) |

---

## 🎮 วิธีเล่น (How to Play)

1. **Slash** — ลากนิ้วบนหน้าจอเพื่อโจมตีศัตรู (ต้องลากระยะพอสมควร)
2. **Red Slash** — เมื่อ Combo ถึง 10, ท่าถัดไปจะเป็นสีแดง โจมตีโดนจะคืน SP
3. **Ultimate** — กดปุ่ม `ULT` เมื่อ Gauge เต็ม เพื่อปล่อย Triple Slash ขนาดใหญ่
4. **Power-Up** — ฟันไอเทมที่ลอยบนหน้าจอเพื่อเก็บเข้า Inventory แล้วกดเพื่อใช้งาน
5. **หลีกเลี่ยง C4** — อย่าฟัน C4 สีเขียวที่กระดอนอยู่ มิฉะนั้นจะเสีย HP หนัก

---

<div align="center">

*พัฒนาเป็นส่วนหนึ่งของรายวิชา CP213*

</div>
