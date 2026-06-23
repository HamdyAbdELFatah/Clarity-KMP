#import <Foundation/Foundation.h>

/**
 * Forward declarations for the official Microsoft Clarity iOS SDK.
 *
 * TARGETS: Microsoft Clarity iOS SDK **3.5.3** (Swift module `Clarity`).
 * Keep this in sync with the `clarity-ios-sdk` version in `gradle/libs.versions.toml`.
 *
 * These interfaces match the Objective-C API surface exposed by the
 * Clarity.xcframework (via its generated `-Swift.h` bridging header).
 *
 * The consuming iOS application MUST link the actual Clarity framework
 * (via CocoaPods `pod 'Clarity'` or Swift Package Manager) so that
 * these symbols resolve at link time.
 *
 * Reference: https://learn.microsoft.com/en-us/clarity/mobile-sdk/ios-sdk
 *
 * UPGRADE PROCEDURE — when bumping the Clarity iOS SDK version:
 *   1. Download the new Clarity.xcframework and read its generated `-Swift.h`
 *      (found inside the framework bundle).
 *   2. Diff every selector, return type, and nullability below against it.
 *      Obj-C dispatches by selector at runtime, so a wrong return type (e.g.
 *      `void` declared as `BOOL`) compiles and links but reads garbage / crashes.
 *   3. Update the `objc_runtime_name` attributes if the Swift class mangling
 *      changed (verify with `nm`/`otool` against the real binary).
 *   4. Update the `clarity-ios-sdk` version in `gradle/libs.versions.toml`
 *      and the TARGETS line above to match.
 *   5. Run `:clarity-kmp:compileKotlinIosSimulatorArm64` and the `iosTest`
 *      smoke test; link the sample app against the new SDK in Xcode.
 */

/**
 * Log level for the Clarity iOS SDK diagnostic logging.
 * Renamed from `LogLevel` to `ClarityLogLevel` in SDK v3.4.0.
 */
typedef NS_ENUM(NSInteger, ClarityLogLevel) {
    ClarityLogLevelVerbose = 0,
    ClarityLogLevelDebug   = 1,
    ClarityLogLevelInfo    = 2,
    ClarityLogLevelWarning = 3,
    ClarityLogLevelError   = 4,
    ClarityLogLevelNone    = 5
};

/**
 * Configuration object for initializing the Clarity iOS SDK.
 */
__attribute__((objc_runtime_name("_TtC7Clarity13ClarityConfig")))
@interface ClarityConfig : NSObject

@property (nonatomic, readonly, nonnull) NSString *projectId;
@property (nonatomic, assign) ClarityLogLevel logLevel;

- (nonnull instancetype)initWithProjectId:(nonnull NSString *)projectId;

@end

/**
 * Main entry point for the Microsoft Clarity iOS SDK.
 *
 * All methods are class-level (static) and return BOOL to indicate success.
 */
__attribute__((objc_runtime_name("_TtC7Clarity10ClaritySDK")))
@interface ClaritySDK : NSObject

+ (BOOL)initializeWithConfig:(nonnull ClarityConfig *)config;
+ (void)pause;
+ (void)resume;
+ (BOOL)isPaused;
+ (BOOL)startNewSessionWithCallback:(void (^ _Nullable)(NSString * _Nonnull))callback;
+ (BOOL)setCustomUserId:(nonnull NSString *)customUserId;
+ (BOOL)setCustomSessionId:(nonnull NSString *)customSessionId;
+ (nullable NSString *)getCurrentSessionUrl;
+ (BOOL)setCurrentScreenName:(nullable NSString *)currentScreenName;
+ (BOOL)sendCustomEventWithValue:(nonnull NSString *)value;
+ (BOOL)setCustomTagWithKey:(nonnull NSString *)key value:(nonnull NSString *)value;
+ (BOOL)setCustomTagWithKey:(nonnull NSString *)key values:(nonnull NSSet<NSString *> *)values;
+ (BOOL)setOnSessionStartedCallback:(void (^ _Nonnull)(NSString * _Nonnull))callback;
+ (BOOL)consentWithAnalyticsStorage:(BOOL)analyticsStorage;

@end
