# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Smart File Manager is a JavaFX-based desktop application that provides intelligent file organization with AI-powered analysis capabilities. The project uses Java 17 and follows an MVC architecture pattern.

## Build Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run
# or
./gradlew runApp

# Run tests and demos
./gradlew statisticsTest      # Test statistics system with UI
./gradlew quickStatsTest      # Quick statistics verification
./gradlew aiDemo              # Run AI analysis demo (no API key needed)
./gradlew monitoringTest      # Test real-time folder monitoring
./gradlew runAllDemos         # Run all demos sequentially

# Utility commands
./gradlew verifyStatisticsFiles   # Check for required statistics files
./gradlew createTestFile          # Generate clean test files
./gradlew createAIDemo            # Create AI demo files
./gradlew helpStatistics          # Show help for statistics testing
```

## Architecture

### MVC Pattern Structure
- **Controllers** (`src/main/java/com/smartfilemanager/controller/`): Handle UI logic and user interactions
  - `MainController.java`: Central controller coordinating all features
  - `SettingsController.java`: Manages application settings
  - `StatisticsController.java`: Handles statistics visualization
  - `EventHandlers.java`: Centralizes event handling logic

- **Models** (`src/main/java/com/smartfilemanager/model/`): Domain objects and data structures
  - `FileInfo.java`: Core file information model
  - `AppConfig.java`: Application configuration model
  - `DuplicateGroup.java`: Groups duplicate files
  - `CleanupCandidate.java`: Represents files for cleanup

- **Services** (`src/main/java/com/smartfilemanager/service/`): Business logic layer
  - `FileScanService.java`: File scanning operations
  - `FileOrganizerService.java`: File organization logic
  - `DuplicateDetectorService.java`: Duplicate detection algorithms
  - `FileWatcherService.java`: Real-time folder monitoring
  - `ConfigService.java`: Configuration management

### Key Features Implementation
1. **AI Analysis**: `util/AIAnalyzer.java` - Uses OpenAI API for file content analysis
2. **Statistics System**: `controller/StatisticsController.java` - Visualizes file data with charts
3. **Real-time Monitoring**: `service/FileWatcherService.java` - Watches folders for changes
4. **Theme Support**: `ui/ThemeManager.java` - Manages light/dark themes
5. **System Tray**: `ui/SystemTrayManager.java` - Minimize to system rtray

### Database
- Uses SQLite for local data persistence
- Database operations handled by `util/DatabaseHelper.java`
- Stores file metadata, scan history, and configuration

### UI Structure
- FXML files in `src/main/resources/fxml/`
  - `main.fxml`: Main application window
  - `settings.fxml`: Settings dialog
  - `statistics.fxml`: Statistics view
- CSS files in `src/main/resources/css/`
  - `styles.css`: Base styles
  - `dark-theme.css`: Dark theme styles
  - `statistics-styles.css`: Statistics-specific styles

## Development Notes

### Dependencies
- JavaFX 17.0.2 (controls, fxml, graphics, base)
- Lombok for reducing boilerplate
- Gson for JSON processing
- SQLite JDBC for database
- SLF4J with Logback for logging
- JUnit 5 for testing

### Key Classes to Understand
1. `SmartFileManagerApp.java`: Application entry point
2. `MainController.java`: Central controller coordinating all features
3. `FileOrganizerService.java`: Core file organization logic
4. `AIAnalyzer.java`: AI integration for file analysis

### Testing Approach
- Demonstration-based testing with standalone test applications
- No traditional unit tests; uses interactive demos instead
- Test files generate synthetic data for UI testing

### Configuration
- Application settings stored in `AppConfig` model
- Default rules in `src/main/resources/config/default-rules.json`
- Internationalization via `messages.properties`

### Platform Considerations
- Primary development on Windows (includes Windows-specific JavaFX runtime)
- System tray and startup integration are Windows-focused
- File paths should use platform-independent approaches when possible