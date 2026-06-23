# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-06-23

### Added

- Injectable `ClarityClient` API with platform factories and lifecycle state reporting.
- Android implementation using the official Microsoft Clarity SDK (`com.microsoft.clarity:clarity:3.x`).
- Full iOS bindings for Clarity 3.5.3; consuming apps link the official SDK with SPM or CocoaPods.
- Session callbacks, pause/resume, session URLs, custom session IDs, multi-value tags, and consent.
- Compose Multiplatform screen tracking helper (`ClarityScreen`).
- Compose event tracking helper (`TrackClarityEventOnFirstComposition`).
- Compose click tracking modifier (`Modifier.clarityClickable`).
- Sample Compose Multiplatform app demonstrating all features.
- Maven Central publishing preparation with Vanniktech Maven Publish plugin.
- Apache 2.0 license.
- Dokka, binary API validation, CI, security, release, audit, and consumer documentation.
