# LinkUp — Backend Setup, Security & Deployment Guide

## Current Runtime Architecture (Android)
This Android application implements a complete relational database layer using **Room (SQLite)** with foreign keys, PBKDF2WithHmacSHA256 salted password hashing, deterministic two-way matching (`MatchingEngine`), real-time reactive Kotlin `Flow` streams, and role-based access control.

## Connecting Multi-Device Cloud Sync (Firebase or Supabase)

### Option A: Supabase (PostgreSQL + Auth + Storage + Realtime)
If deploying a shared multi-device backend using Supabase:
1. Create a Supabase project at `https://supabase.com`.
2. Provision the normalized relational tables (`users`, `user_profiles`, `skills`, `user_skills`, `education_records`, `experience_records`, `founder_preferences`, `job_listings`, `job_applications`, `startup_opportunities`, `match_records`, `connection_requests`, `conversations`, `messages`, `notifications`, `verification_submissions`, `trial_collaborations`, `collaboration_tasks`, `collaboration_reviews`, `reports`, `country_regions`) with Row Level Security (RLS) policies scoping private PII (`email`, `phone`, `messages`) to authenticated owners and participants.
3. Configure environment secrets in the **Secrets panel in AI Studio** (never commit keys to source):
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
4. Enable **Google OAuth** and **Phone OTP** providers in Supabase Authentication Settings before exposing those sign-in buttons in the UI.

### Option B: Firebase Firestore & Google Sign-In
1. Provision Firebase in AI Studio or the Firebase Console with Firestore and Google Sign-In enabled.
2. Add `google-services.json` under `app/` and deploy `firestore.rules` enforcing per-user isolation and role-based admin checks.

## Android Build & Release Instructions
1. Verify unit and Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
2. Build debug or release APK / AAB:
   ```bash
   gradle :app:assembleDebug
   gradle :app:bundleRelease
   ```
3. In Google AI Studio, use the **Export / Build APK / AAB** menu to generate signed artifacts for testing or Play Console submission.
