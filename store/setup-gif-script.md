# Setup GIF — recording script

The single highest-leverage asset for conversion. It replaces the wall of text explaining
the ADB step with ~15 seconds of "oh, that's it?"

**Target:** `docs/assets/setup.gif`. Since 2026-10-23 the landing page shows the static
terminal stand-in directly (the old auto-detect `<img>` caused a 404 on every visit). When the
recording exists, add the file and swap the `#gif-placeholder` block in `docs/index.html` for
an `<img src="assets/setup.gif" width=… height=… alt="…">`.

**Specs**
- 1280×720 or 1000×560, whichever keeps text crisp
- Under 3 MB — it loads above the fold on mobile
- 12–18 seconds, looping, no audio
- Real time. Do **not** speed it up. The entire argument is that this is genuinely short;
  a sped-up video quietly implies it isn't.

---

## Shot list

| # | Duration | What's on screen |
|---|----------|------------------|
| 1 | 0:00–0:02 | Empty terminal, large font. Title card overlay: **"The entire setup."** |
| 2 | 0:02–0:04 | Type `adb devices`, Enter. Device serial appears. |
| 3 | 0:04–0:07 | Paste the grant command in one motion — a paste, not a keystroke-by-keystroke type. It should look like what it is: a copy-paste. |
| 4 | 0:07–0:09 | Enter. Command returns with no output. Beat of silence. |
| 5 | 0:09–0:12 | Overlay: **"That's it. Unplug."** |
| 6 | 0:12–0:15 | Cut to the phone: open Instagram → screen desaturates → swipe to Camera → color returns. |

Shot 6 is what makes the gif worth watching. Steps 1–5 defuse the objection; step 6 shows
the payoff. Don't ship it with only the terminal half.

**The command, verbatim:**

```
adb shell pm grant com.graygate android.permission.WRITE_SECURE_SETTINGS
```

---

## Details that matter

- **Font size way up** in the terminal — 18–20pt. It will be viewed at ~400px wide on a phone.
- **Clean prompt.** No `~/dev/some/deep/path (git:main*)` — set `PS1='$ '` for the recording.
  A hobbyist-looking prompt reads as "this is a developer tool," which is the exact
  impression you're trying to remove.
- **Light terminal theme** if the landing page section is light; dark reads as "hacker."
  The page's placeholder uses a dark terminal, so either works — just be consistent.
- **Blur or fake the device serial** in shot 2. It's a real identifier.
- **Show a real phone** in shot 6, not an emulator. Emulator chrome undercuts the claim.
- Keep the mouse cursor out of frame.

## Recording

Any of these work:
- **macOS:** QuickTime screen recording → `ffmpeg` to gif
- **Windows:** ScreenToGif (records and exports directly, easiest option)
- **Linux:** Peek, or `wf-recorder` → `ffmpeg`

`ffmpeg` conversion with a decent palette (keeps the text readable and the file small):

```bash
ffmpeg -i setup.mov -vf "fps=12,scale=1000:-1:flags=lanczos,palettegen" -y palette.png
ffmpeg -i setup.mov -i palette.png -lavfi "fps=12,scale=1000:-1:flags=lanczos[x];[x][1:v]paletteuse" -y docs/assets/setup.gif
```

If it lands over 3 MB, drop to `fps=10` before you drop the resolution — text legibility
matters more than smoothness here.

## Also worth cutting from the same recording

- A **9:16 vertical** version for the Play Store screenshot slot (shot 6 only).
- A **still frame** of the terminal mid-command for screenshot #3 in the listing plan.
