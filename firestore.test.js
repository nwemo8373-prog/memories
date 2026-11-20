const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Negm User",
    username: "negm_user",
    email: "negm@example.com",
    photo: "",
    language: "ar",
    sensitivity: 75,
    holdDurationMs: 500,
    soundEnabled: true,
    vibrationEnabled: true,
    batterySaver: false,
    preferredCamera: "front",
    handPreference: "both",
    pauseOnLeave: true,
    syncHistoryEnabled: false,
    activeProfileId: "default_profile",
    totalCommandsExecuted: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validGesture(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    gestureKey: "THUMBS_UP",
    displayNameAr: "رفع الإبهام",
    displayNameEn: "Thumbs Up",
    descriptionAr: "رفع الإبهام لأعلى لتشغيل الفلاش",
    descriptionEn: "Raise thumb up to turn flashlight on",
    actionKey: "FLASH_ON",
    enabled: true,
    custom: false,
    sensitivity: 75,
    holdDurationMs: 500,
    createdAt: now,
    updatedAt: now,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write any user data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("gestures").get());
});

test("Authenticated owner can create, partial-update, and read profile; rejects ghost fields & immutable edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  // Settings partial update
  await assertSucceeds(
    profileRef.update({
      sensitivity: 85,
      language: "en",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Command counter partial update
  await assertSucceeds(
    profileRef.update({
      totalCommandsExecuted: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId 'id' leakage
  await assertFails(
    aliceDb.collection("users").doc("alice_ghost").set({
      ...validProfile("alice_ghost"),
      isAdmin: true,
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's profile, gestures, profiles, or logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gestures").doc("g1").set(validGesture(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("gestures").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("gestures").doc("g1").update({
      actionKey: "FLASH_OFF",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can manage gestures, control profiles, and activity logs; enforces bounds and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1500);

  // Gesture create + update + list
  const gestureRef = aliceDb.collection("users").doc(ALICE_UID).collection("gestures").doc("THUMBS_UP");
  await assertSucceeds(gestureRef.set(validGesture(ALICE_UID)));
  await assertSucceeds(
    gestureRef.update({
      actionKey: "OPEN_APP",
      targetAppPackage: "com.android.settings",
      targetAppName: "Settings",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("gestures").get());

  // Reject out-of-bounds sensitivity
  await assertFails(
    gestureRef.update({
      sensitivity: 150,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Control profile create + update
  const profRef = aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("default_profile");
  await assertSucceeds(
    profRef.set({
      userId: ALICE_UID,
      name: "Default",
      defaultProfile: true,
      mappings: { THUMBS_UP: "FLASH_ON", OPEN_PALM: "FLASH_OFF" },
      createdAt: now,
      updatedAt: now,
    })
  );

  // Activity log create (immutable: update must fail)
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("activityLogs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      gestureKey: "THUMBS_UP",
      gestureLabel: "Thumbs Up",
      actionKey: "FLASH_ON",
      actionLabel: "Flashlight On",
      succeeded: true,
      detailMessage: "Torch enabled",
      confidence: 94,
      createdAt: now,
    })
  );
  await assertFails(logRef.update({ succeeded: false }));
});
