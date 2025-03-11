<a href="https://ibb.co/Ps1bdt22"><img src="https://i.ibb.co/Ps1bdt22/AppLogo.png" alt="AppLogo" border="0"></a>

# BudgetSmart: Personal Finance Tracking App

## Overview

BudgetSmart is a comprehensive Android application designed to help users easily track and manage their personal finances. Built with simplicity and functionality in mind, the app provides intuitive tools for monitoring expenses, managing budgets, and gaining insights into your financial habits.

## Features

### 💰 Expense and Income Tracking
- Add transactions with detailed information
- Categorize expenses and income
- Track spending across multiple categories

### 📊 Budget Management
- Set monthly budgets for different spending categories
- Visual progress tracking for budget limits
- Real-time budget utilization indicators

### 📈 Financial Insights
- Monthly and yearly financial summaries
- Detailed reports with pie charts and bar graphs
- Category-wise spending analysis

### 🔒 Secure Authentication
- Email/Password registration
- Google Sign-In integration
- Secure Firebase authentication

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Database**: Firebase Firestore
- **Authentication**: Firebase Authentication
- **UI**: Material Design Components
- **Charting**: MPAndroidChart

## Screenshots

<a href="https://ibb.co/DHm7B2nV"><img src="https://i.ibb.co/NdfL0BzV/Whats-App-Image-2025-03-11-at-21-04-24.jpg" alt="Whats-App-Image-2025-03-11-at-21-04-24" border="0"></a>

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Google Firebase account
- Minimum SDK version: 23

### Installation

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/BudgetSmart.git
   ```

2. Open the project in Android Studio

3. Set up Firebase
   - Create a new Firebase project
   - Add an Android app to your Firebase project
   - Download and add the `google-services.json` to your project
   - Enable Firestore and Authentication in your Firebase console

4. Sync Gradle and run the app

## Firebase Configuration

Ensure the following Firebase services are enabled:
- Firestore Database
- Authentication (Email/Password and Google Sign-In)

## Key Dependencies

- Firebase Firestore
- Firebase Authentication
- Hilt for Dependency Injection
- Coroutines for asynchronous programming
- MPAndroidChart for visualizations

## Project Structure

```
com.example.budgetsmart2/
│
├── data/                   # Repository implementations
│   └── repositoriesImp/
│
├── di/                     # Dependency injection modules
│
├── domain/                 # Business logic and data models
│   ├── dataClasses/
│   ├── enums/
│   └── repositories/
│
└── presentation/           # UI and ViewModels
    ├── adapters/
    ├── dialogs/
    ├── fragments/
    └── viewModels/
```

## Acknowledgements

- [Firebase](https://firebase.google.com/)
- [Android Jetpack](https://developer.android.com/jetpack)
- [Material Design](https://material.io/develop/android)
