# Trust System Package

This package implements a trust-based message evaluation system for the emergency communication network.

## Quick Start

### 1. Initialize Managers

```kotlin
val trustManager = TrustManager(context)
val calculator = MessageTrustCalculator(trustManager)
val verificationManager = MessageVerificationManager(context)
```

### 2. Set Trust Levels

```kotlin
// Import known contacts (automatic level 1)
trustManager.importKnownContacts(listOf("alice", "bob", "charlie"))

// Set higher trust for close contacts
trustManager.setTrustLevel("mom", ContactTrustLevel.CLOSE_FAMILY)
trustManager.setTrustLevel("best_friend", ContactTrustLevel.FRIEND)
```

### 3. Evaluate Messages

```kotlin
val evaluation = calculator.evaluateMessage(
    messageId = message.messageId,
    originalSenderId = message.sourceId,
    hopCount = message.hopCount,
    verifications = verificationManager.getVerifications(message.messageId)
)

println("Trust Score: ${evaluation.overallTrustScore}") // 0.0 - 1.0
println("Rating: ${MessageTrustEvaluation.getTrustRating(evaluation.overallTrustScore)}")
println("Emoji: ${MessageTrustEvaluation.getTrustEmoji(evaluation.overallTrustScore)}")
```

### 4. Add Verifications

```kotlin
// User confirms message is true
verificationManager.addVerification(
    messageId = "msg123",
    verifierId = currentUserId,
    isConfirmed = true,
    comment = "I can confirm this is accurate"
)

// User rejects message as false
verificationManager.addVerification(
    messageId = "msg456",
    verifierId = currentUserId,
    isConfirmed = false,
    comment = "This is incorrect"
)
```

## Files

- **ContactTrustLevel.kt** - Data class for contact trust levels (0-3)
- **MessageTrustEvaluation.kt** - Data class for message trust evaluation results
- **MessageVerification.kt** - Data class for message confirmations/rejections
- **TrustManager.kt** - Manages contact trust levels with persistence
- **MessageTrustCalculator.kt** - Calculates trust scores using the algorithm
- **MessageVerificationManager.kt** - Manages message verifications with persistence
- **TrustSystemExample.kt** - Example integration code

## Trust Levels

| Level | Name | Trust Factor | Usage |
|-------|------|--------------|-------|
| 0 | Unknown | 0.0 | Not in contacts |
| 1 | Known Contact | 0.33 | From phone/email/messenger |
| 2 | Friend | 0.67 | Manually set |
| 3 | Close/Family | 1.0 | Manually set |

## Trust Score Range

| Score | Rating | Emoji | Meaning |
|-------|--------|-------|---------|
| 0.80+ | Very High | ✅ | Highly trustworthy |
| 0.60-0.79 | High | 👍 | Trustworthy |
| 0.40-0.59 | Medium | ⚠️ | Caution advised |
| 0.20-0.39 | Low | ⚡ | Be skeptical |
| 0.00-0.19 | Very Low | ❌ | Highest caution |

## Algorithm

The trust score combines:
1. **Sender Trust (60%)** - Trust level of the original sender
2. **Hop Count (40%)** - Number of intermediate devices (fewer = better)
3. **Verifications (±15%)** - Confirmations/rejections weighted by verifier trust

See [TRUST_SYSTEM.md](../../../../../TRUST_SYSTEM.md) for detailed documentation.

## Testing

Run unit tests:
```bash
./gradlew test --tests "com.fourpeople.adhoc.trust.*"
```

Test files:
- `TrustManagerTest.kt` - 20 test cases
- `MessageTrustCalculatorTest.kt` - 25 test cases
- `MessageVerificationManagerTest.kt` - 20 test cases
- `TrustDataClassesTest.kt` - 30 test cases
