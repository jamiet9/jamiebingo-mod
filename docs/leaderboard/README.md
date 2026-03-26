# JamieBingo Leaderboard Page

This folder is a static leaderboard frontend intended for GitHub Pages.

## What it does

- Reads run data from `./data/submissions.json` by default
- Supports filtering by player, validity, board size, and sort order
- Shows valid vs invalid runs using the same eligibility rules as the mod submit screen

## Important limitation

GitHub Pages cannot receive or store uploads directly.

That means the mod should submit runs to a real backend endpoint, and that backend should then:

1. store the submissions
2. expose JSON for the page to read

## Mod config

The mod-side config file is:

`config/jamiebingo_leaderboard.json`

Example:

```json
{
  "submitUrl": "https://your-backend.example.com/api/jamiebingo/submissions",
  "leaderboardUrl": "https://yourname.github.io/jamiebingo/leaderboard/",
  "apiKey": "",
  "timeoutSeconds": 12
}
```

## GitHub Pages setup

1. Put this `leaderboard` folder into the repo that will host your Pages site.
2. Enable GitHub Pages for that repo.
3. If you are serving static JSON from the same repo, keep updating `data/submissions.json`.
4. If your backend serves JSON elsewhere, open the page with:

`?source=https://your-backend.example.com/api/jamiebingo/submissions.json`

Example:

`https://yourname.github.io/jamiebingo/leaderboard/?source=https://your-api.example.com/submissions`

## Accepted submission shape

The page accepts either:

- a raw array of submissions
- or an object with `submissions: [...]`

Each submission should look like:

```json
{
  "playerName": "Runner",
  "cardSeed": "seed",
  "worldSeed": "seed",
  "durationSeconds": 1234,
  "finishedAtEpochSeconds": 1774406400,
  "completed": true,
  "participantCount": 1,
  "commandsUsed": false,
  "rerollsUsedCount": 0,
  "fakeRerollsUsedCount": 0,
  "previewSize": 5,
  "teamColorId": 0,
  "settingsLines": ["Mode: FULL"]
}
```
