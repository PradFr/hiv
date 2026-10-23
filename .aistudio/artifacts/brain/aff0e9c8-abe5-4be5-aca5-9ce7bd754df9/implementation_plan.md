# DuoSpace — Answers to Your Questions & Sync Plan

## First: Answers to Every Question You Asked (in Plain English)

### 1. What does "Media Relay" even mean?
Think of a **Post Office Locker** vs. a **Storage Warehouse**:
- **Normal Cloud Storage (Warehouse):** Every photo or video you send stays on the server forever. On Supabase's free 1 GB plan, sending ~150 photos or a few videos would fill up your 1 GB limit and stop working.
- **Media Relay (Post Office Locker — What Signal & WhatsApp do):**
  1. When you send a photo in Chat or upload a file to The Vault, your phone locks (encrypts) the file and drops it into Supabase's temporary folder.
  2. The moment the other person opens the app, their phone downloads that locked file into their app's private storage and **immediately deletes the copy from Supabase**.
  3. **Why this is awesome:** Your Supabase storage goes right back down to **0 MB** after every delivery! You can send thousands of photos and files for free without ever hitting Supabase's 1 GB limit.

---

### 2. In Signal, how can I see photos offline once downloaded, yet it doesn't seem to take up phone storage?
Here is the exact secret behind how Signal does that:
1. **Yes, once downloaded, the photo is stored inside the app on your phone** — that is the *only* way any app can show you a photo when your internet is turned off.
2. **Why does it feel like it takes zero space on your phone?**
   - **95% Smart Shrinking:** A modern phone camera takes huge **8 MB to 15 MB** photos. Before sending, Signal automatically shrinks standard photos down to **~250 KB (0.25 MB)**. On a phone screen, it looks identical, but it takes **40× less space**. That means **1,000 photos only use ~250 MB** (less than one short video!).
   - **No Duplicate Gallery Copies:** Because the photo stays locked inside the app's private folder (instead of saving a second copy to your phone's Gallery / Google Photos), it never clutters your phone storage unless you explicitly tap **"Save to Phone"**.

---

### 3. Which backend is easiest to set up (and what does each option actually do)?

| Option | What It Does | What YOU Have to Do | Free Limit & Catch |
| :--- | :--- | :--- | :--- |
| **1. Supabase + Post-Office Relay (Recommended)** | Syncs Chat, Journal, Wishlist, and Vault between your two phones. Uses the Post-Office Relay trick so Supabase's free 1 GB never fills up. | Create a free project on `supabase.com` and paste 2 keys (`SUPABASE_URL` & `SUPABASE_ANON_KEY`) into AI Studio's Secrets panel. I write 100% of the code. | **100% Free** (no credit card needed). |
| **2. Built-in Firebase (1-Click inside AI Studio)** | Syncs Chat, Journal, Wishlist, and small compressed photos using Google's built-in database inside AI Studio. | **Zero external setup.** You just click "Accept" on a popup right here in AI Studio—no website signup or API keys needed. | Great for text & compressed photos, but large raw video files require a Google Cloud billing account. |
| **3. Cloudflare R2** | Stores up to 10 GB of large videos/files permanently in the cloud instead of deleting them after download. | Create a Cloudflare account, enter a credit card (even for the free tier), and paste 4 keys. | 10 GB free, but requires a credit card to activate. |

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Look over the 3 recommended choices below.
> - If you like the **Recommended** setup below, just click **Proceed** and I will build it right away.
> - If you prefer one of the other options (for example, **Built-in Firebase** so you don't have to create a Supabase account, or changing how photos are compressed), just reply in the chat and tell me which one you want!

- **Choice 1 — Backend Setup**:
  - **Recommended Default**: **Supabase + Post-Office Relay** (with automatic fallback to local offline mode until you paste your `SUPABASE_URL` and `SUPABASE_ANON_KEY` in the Secrets panel).
  - *Alternative*: Tell me if you want **1-Click Built-in Firebase** instead.
- **Choice 2 — Photo & File Size Handling**:
  - **Recommended Default**: **Signal-style smart compression (~250 KB) for Chat & Wishlist photos** so they load instantly and barely use any phone space, while giving you a choice in **The Vault** between *Compressed Photo* or *Original File*.
- **Choice 3 — Cloud Cleanup**:
  - **Recommended Default**: **Auto-delete encrypted files from the cloud locker** as soon as the other person's phone has downloaded them into their app's offline sandbox, keeping your cloud usage near 0 MB.

---

## 1. Overview & Core Concept

- **What It Does**: Adds Signal-style image optimization and cloud relay sync to **DuoSpace** so both people can chat, write in the daily two-tab Journal, add items to the Wishlist, and share files in The Vault—with full offline access once downloaded and near-zero cloud storage footprint.
- **Target Audience**: Two partners sharing a private, distraction-free space with clean icons and zero technical jargon in the UI.
- **Key Value**: Fast offline access to every shared photo and file without cluttering the phone's Gallery or running out of free cloud storage.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Sending Photos in Chat / Wishlist**: Photos are automatically optimized (~250 KB) in the background before encryption, so sending and viewing is instant and uses minimal phone space.
  2. **Uploading to The Vault**: Tapping **Upload** lets you pick any photo, video, or document into your folder (`Alex` or `Sam` -> `Images`, `Videos`, `Audio`, `Documents`), encrypts it into the app's private offline sandbox, and queues it for your partner's phone.
  3. **Offline Viewing & Save to Phone**: Every downloaded photo or file opens instantly inside the app even without internet, and stays completely hidden from the phone's Gallery unless you tap **Save to Phone**.
- **Visual Identity & Theme**:
  - Clean, cozy dark aesthetic with an icon-only bottom navigation bar (`Home`, `Chat`, `Journal`, `Wishlist`, `Vault`).
  - Zero technical/security jargon badges—everything works silently in the background.
  - Whole-app Recent Apps screen blur/cover while still allowing normal screenshots and screen recording inside the app.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Signal-Style Smart Photo Compression**
  - *Chosen Approach*: Downscale and compress standard camera images to ~250–350 KB (1600px max dimension, 82% JPEG/WebP quality) before encrypting.
  - *Why*: Reduces storage usage by ~95% on the phone and makes transfers 20× faster while looking identical on mobile screens.
- **Decision 2: Post-Office Relay (Store-and-Forward) for Media**
  - *Chosen Approach*: Keep lightweight text (Chat, Journal, Wishlist, folder names) in the database, and automatically purge encrypted media blobs from the storage bucket once the partner's device confirms download.
  - *Why*: Prevents the 1 GB free tier from ever filling up.

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────────────┐
│                        YOUR PHONE (DuoSpace)                         │
│                                                                      │
│  ┌────────────────────┐    ┌──────────────────────────────────────┐  │
│  │ Smart Image Sizer  │───▶│ Private Offline Sandbox              │  │
│  │ (10 MB ──▶ 250 KB) │    │ (Encrypted on phone, works offline,  │  │
│  └────────────────────┘    │  invisible to Gallery & other apps)  │  │
│                            └─────────────────┬────────────────────┘  │
└──────────────────────────────────────────────┼───────────────────────┘
                                               │
                           1. Upload locked    │  2. Partner downloads
                              file to locker   │     & locker empties
                                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   SUPABASE CLOUD (Free 1 GB Tier)                    │
│                                                                      │
│  ┌─────────────────────────────┐   ┌──────────────────────────────┐  │
│  │ Lightweight Database (<5MB) │   │ Temporary Media Locker       │  │
│  │ • Chat messages             │   │ • Holds locked file ONLY     │  │
│  │ • Daily Journal entries     │   │   until partner's phone      │  │
│  │ • Wishlist items & links    │   │   downloads it, then deletes │  │
│  │ • Vault folder list         │   │   it (stays near 0 MB used)  │  │
│  └─────────────────────────────┘   └──────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```
