const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated access is denied across user profile and subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("app_rules").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
});

test("Owner can create and partially update UserProfile, while non-owner and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      keysBalance: 3,
      plapsBalance: 15,
      dailyStreak: 1,
      lastSessionDate: "2026-11-19",
      lastBurnMonth: "2026-11",
      squatCountTarget: 20,
      selectedFontKey: "playfair",
      selectedFontColorHex: "#B8255D",
      wallpaperKey: "coquette_silk",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      keysBalance: 2,
      plapsBalance: 20,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Cross-user read & write fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      keysBalance: 999,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("AppRules, Sessions (with terminal locking), UsageLogs, CustomTasks, and AiSuggestions obey strict rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // AppRule create & update & list
  const ruleRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("app_rules")
    .doc("com_example_browser");

  await assertSucceeds(
    ruleRef.set({
      userId: ALICE_UID,
      packageName: "com.android.chrome",
      appName: "Browser",
      category: "Social",
      necessaryApp: false,
      restrictedInSession: true,
      scheduleEnabled: true,
      scheduleStartMinutes: 1320,
      scheduleEndMinutes: 0,
      dailyLimitEnabled: true,
      dailyLimitMinutes: 60,
      usedTodayMinutes: 25,
      tempUnblockUntilMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("app_rules").get()
  );

  // Session create + pause + complete + terminal lock
  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("sess_1");

  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      mode: "SESSION",
      durationMinutes: 120,
      startedAtMillis: 1700000000000,
      endsAtMillis: 1700007200000,
      pausedUntilMillis: 0,
      status: "ACTIVE",
      earlyExitMethod: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    sessionRef.update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: cannot update once COMPLETED
  await assertFails(
    sessionRef.update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // UsageLogs create & list
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("usage_logs")
      .doc("log_1")
      .set({
        userId: ALICE_UID,
        packageName: "com.android.chrome",
        appName: "Browser",
        dateKey: "2026-11-19",
        dayOfWeek: 4,
        hourOfDay: 22,
        screenTimeMinutes: 45,
        restrictedMinutes: 60,
        createdAt: now,
        updatedAt: now,
      })
  );

  // CustomTasks create & cross-user denial
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("custom_tasks")
      .doc("task_1")
      .set({
        userId: ALICE_UID,
        taskType: "TYPING_PROMPT",
        content: "I choose calm focus, elegance, and poise today.",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("custom_tasks").get()
  );

  // AiSuggestions create & apply
  const sugRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("ai_suggestions")
    .doc("sug_1");

  await assertSucceeds(
    sugRef.set({
      userId: ALICE_UID,
      appName: "Browser",
      packageName: "com.android.chrome",
      suggestedStartMinutes: 1320,
      suggestedEndMinutes: 60,
      suggestedDailyLimitMinutes: 45,
      reason: "Late-night spike detected between 22:00 and 01:00.",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    sugRef.update({
      status: "APPLIED",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // SchedulePlans create, update, list, and cross-user denial
  const schedRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("schedules")
    .doc("sched_night");

  await assertSucceeds(
    schedRef.set({
      userId: ALICE_UID,
      name: "Night Wind-Down",
      description: "Sleep cozy without late scrolling",
      enabled: true,
      startMinutes: 1320,
      endMinutes: 360,
      daysOfWeek: [1, 2, 3, 4, 5],
      modeType: "BLOCK_SELECTED",
      allowedPackages: ["com.android.dialer"],
      restrictedPackages: ["com.mobile.legends", "com.android.chrome"],
      customBlockReason: "Sweet dreams! Apps unlock in the morning.",
      unlockedUntilMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    schedRef.update({
      modeType: "ALLOW_ONLY_SELECTED",
      allowedPackages: ["com.android.dialer", "com.google.android.apps.messaging"],
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("schedules").get()
  );
});

test("Global Challenge Leaderboard permits authenticated reads and strictly isolates user writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);
  const weekKey = "2026-W47";
  const aliceDocId = `${weekKey}_${ALICE_UID}`;

  // Unauthenticated read & write denied
  await assertFails(
    unauthDb.collection("challenge_leaderboard").where("weekKey", "==", weekKey).get()
  );
  await assertFails(
    unauthDb.collection("challenge_leaderboard").doc(aliceDocId).set({
      userId: ALICE_UID,
      weekKey,
      challengeId: "pink_poise_sprint",
      challengeTitle: "Silk & Serenity Sprint",
      displayName: "Alice",
      tasksCompleted: 2,
      restrictedMinutes: 120,
      dailyStreak: 3,
      totalScore: 295,
      badgeTitle: "Glitter Star",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid owner create succeeds
  await assertSucceeds(
    aliceDb.collection("challenge_leaderboard").doc(aliceDocId).set({
      userId: ALICE_UID,
      weekKey,
      challengeId: "pink_poise_sprint",
      challengeTitle: "Silk & Serenity Sprint",
      displayName: "Alice",
      tasksCompleted: 2,
      restrictedMinutes: 120,
      dailyStreak: 3,
      totalScore: 295,
      badgeTitle: "Glitter Star",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Authenticated peer Bob can read/list global leaderboard
  await assertSucceeds(
    bobDb.collection("challenge_leaderboard").where("weekKey", "==", weekKey).get()
  );

  // Bob cannot overwrite Alice's leaderboard entry
  await assertFails(
    bobDb.collection("challenge_leaderboard").doc(aliceDocId).update({
      totalScore: 99999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot forge a doc ID for another user or mismatch weekKey
  await assertFails(
    bobDb.collection("challenge_leaderboard").doc(`wrong_id_${BOB_UID}`).set({
      userId: BOB_UID,
      weekKey,
      challengeId: "pink_poise_sprint",
      challengeTitle: "Silk & Serenity Sprint",
      displayName: "Bob",
      tasksCompleted: 1,
      restrictedMinutes: 60,
      dailyStreak: 1,
      totalScore: 135,
      badgeTitle: "Silk Novice",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can partially update her own leaderboard score
  await assertSucceeds(
    aliceDb.collection("challenge_leaderboard").doc(aliceDocId).update({
      tasksCompleted: 4,
      restrictedMinutes: 240,
      totalScore: 515,
      badgeTitle: "Royal Princess",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
