# Implementation Plan: Shared Note Board UI Upgrade & Unified Monthly Moments Chat

## Completed Changes

### 1. High-Craft Studio Note Board (`CoupleNoteBoxView`)
- **Dot-Grid Architectural Canvas**: Replaced flat canvas with an interactive dot-grid studio surface (296dp height with responsive sizing) and warm paper tone.
- **Sleek Side-Flip Header**:
  - Animated 3D flip pill button (`rotate_note_box_symbol`) to switch between Partner's Note Board and My Note Studio.
  - Distinct status badges (`EDITING` vs `FOR YOU`) and clear subtitle instructions.
  - Quick clean button (`clean_my_note_button`) with confirmation dialog (accessible only on your own note).
- **Tactile Sticky Notes**:
  - Masking tape header strip accent on each note.
  - Soft pastel color palette (Warm Butter, Soft Rose, Mint Sage, Sky Mist, Lavender, Peach Cream).
  - Tap sticky note to select; shows inline edit text button, delete badge, and corner handle for fluid resize.
  - Drag anywhere on the note to reposition.
- **Polaroid-Framed Photo Cards**:
  - Warm white frame with washi tape strip accent.
  - Corner handle for fluid resizing and drag gesture for positioning.
- **Multi-Color Doodle Studio**:
  - Inline palette of vibrant pens (Rose Pink, Violet, Ocean Blue, Emerald, Amber, Coral Red, Charcoal Ink).
  - Undo stroke capability.
  - Full-layer Move and Zoom/Scale handles (`note_move_resize_doodle_button`) with visual boundary overlay.
- **Floating Glassmorphic Studio Dock**:
  - Sleek pill toolbar at the bottom: `+ Sticky`, `+ Photo`, `Doodle`, `Move Art`.

### 2. Unified Monthly Moments with Full Main Chat Interface
- **Exact Chat Interface**:
  - Opening **My Monthly Moments** or **Partner's Monthly Moments** (when unlocked) renders the exact same `ChatSpaceScreen` UI as Main Chat.
  - Full support for text messages, voice audio recordings with waveforms, stickers, on-screen doodles, camera studio photos/videos, couple cards, polls, location, files, contacts, emoji reactions, replies, message pins, inline search, and markdown export.
- **Call Feature Omitted**:
  - The call icon in the top header is hidden in Monthly Moments.
  - Call History and Auto-Record Calls options are hidden from the 3-dot menu in Monthly Moments.
- **3-Dot Menu "Clear Chat & Lock"**:
  - Added option in the 3-dot menu of Monthly Moments to clear all messages, media, and doodles and immediately relock the chat.
- **Data Isolation & Persistence**:
  - Added dedicated `MessageEntity` and screen doodle storage in `MonthlyMomentsPersistence.kt` keyed by space code and user slot.
  - Kept secret from the partner until unlocked at the end of the month (or via test unlock).
