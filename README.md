<div align="center">

# ⚡ HAVOC

**Мод клиентского уровня для Minecraft на базе Fabric** —
модульная система, собственный рендер-движок и GUI.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-8BC34A?style=for-the-badge&logo=minecraft&logoColor=white)](https://minecraft.net)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.3-D8A657?style=for-the-badge&logo=fabric&logoColor=white)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)

*305 классов · 96 модулей · 60 миксинов · 460+ ресурсов*

</div>

---

## ✨ Что внутри

| | |
|---|---|
| 🧩 **Модульная система** | 96 модулей с настройками (слайдеры, переключатели, режимы) и биндами клавиш |
| ⚔️ **Combat / Movement / Player** | 48 игровых модулей: бой, передвижение, взаимодействие с миром |
| 🌈 **Visuals** | 27 визуальных модулей — от TargetESP с 4 режимами: Маркер, Кольцо, Призраки, Скан-брекет до кастомных партиклов |
| 🎨 **Рендер-движок** | Собственные пайплайны, 80 шейдеров (`.fsh` / `.vsh` / `.glsl`), Kawase-blur, post-processing |
| 🔤 **MSDF-шрифты** | Векторный рендер текста через MSDF-атласы (`.otf` / `.ttf`) — чётко на любом зуме |
| 🖥️ **GUI** | Собственные экраны и меню, система тем (`ThemeManager`) |
| 🌸 **Косметика** | Гео-модели, текстуры плащей и предметов, эффекты убийств, мировые партиклы |
| 💬 **Discord RPC** | Rich Presence через собственный IPC-клиент |
| 🔧 **Инфраструктура** | Конфиги, аккаунты, команды, уведомления, событийная шина на Guava EventBus |

## 🧭 Модули по категориям

| Категория | Модулей | Описание |
|---|:---:|---|
| `visuals` | 27 | ESP, маркеры, партиклы, косметика, анимации |
| `misc` | 21 | утилиты и прочие удобства |
| `combat` | 18 | боевая механика |
| `player` | 15 | взаимодействие игрока с миром |
| `movement` | 15 | передвижение и физика |

## 🗂 Структура проекта

```
Havoc/
├── src/main/
│   ├── java/havoc/
│   │   ├── Havoc.java               # точка входа мода
│   │   ├── module/                  # модульная система (111 файлов)
│   │   │   ├── base/                #   Module, ModuleInfo, категории
│   │   │   ├── bind/                #   биндинги клавиш
│   │   │   ├── setting/             #   типы настроек
│   │   │   └── impl/                #   96 модулей в 5 категориях
│   │   ├── mixin/                   # 60 миксинов в код Minecraft
│   │   ├── render/                  # рендер-движок: пайплайны, билдеры, шрифты
│   │   ├── screen/                  # GUI-экраны
│   │   ├── utilites/                # утилиты (рендер, цвет, математика)
│   │   ├── event/                   # события (EventBus)
│   │   ├── cosmetic/                # косметика: гео-модели, реестр
│   │   ├── config/                  # конфигурация и темы
│   │   ├── discord/                 # Discord IPC / Rich Presence
│   │   ├── notification/            # уведомления
│   │   ├── command/                 # клиентские команды
│   │   └── account/                 # менеджер аккаунтов
│   └── resources/
│       ├── fabric.mod.json          # манифест мода
│       ├── havoc.mixins.json        # конфиг миксинов
│       ├── havoc-renderer.mixins.json
│       ├── havoc.accesswidener
│       └── assets/havoc/            # 458 ресурсов: текстуры, шейдеры, звуки, шрифты
├── build.gradle
├── gradle.properties                # версии Minecraft / Fabric / API
└── gradle/wrapper/                  # Gradle 9.5.1
```

## 🚀 Запуск

**Требования:** JDK 25+

```bash
# клонировать
git clone https://github.com/poctupkosasa-maker/Havoc.git
cd Havoc

# запустить клиент с модом (разработка)
./gradlew runClient

# собрать jar (появится в build/libs/)
./gradlew build
```

> Первая сборка скачает Minecraft и зависимости через Fabric Loom —
> это может занять несколько минут.

## ⚙️ Технический стек

| Компонент | Версия |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.156.0+26.2 |
| Fabric Loom | 1.17 |
| Java | 25 |
| Lombok | 1.18.42 |

---

<div align="center">

*⚠️ Проект размещён в исследовательских целях.*
*Havoc · 2026*

</div>
