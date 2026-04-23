# Contributing

Thanks for your interest in contributing to `@2060.io/react-native-eid-reader`! This document describes the workflow for developing, testing and submitting changes.

## Development workflow

This project uses [Yarn workspaces](https://yarnpkg.com/features/workspaces) and ships a fully working example app under `example/` that consumes the library directly from `src/`.

To get started, clone the repo and install dependencies from the **root** of the project:

```sh
yarn
```

### Running the example app

The example app is the easiest way to develop and test the library. Any change you make in `src/` will be picked up by Metro via workspaces.

```sh
# Start Metro
yarn example start

# In a separate terminal, run the app
yarn example android
# or (after `cd example/ios && pod install`)
yarn example ios
```

See the [example README](./example/README.md) and the [React Native environment setup guide](https://reactnative.dev/docs/environment-setup) if you need help setting up Android Studio / Xcode.

### Editing native code

- **Android** sources live in `android/src/main/java/...`. Open `example/android` in Android Studio to get full IDE support while editing the library code.
- **iOS** sources live in `ios/`. Open `example/ios/NfcPassportReaderExample.xcworkspace` in Xcode after running `pod install`.

## Scripts

The following scripts are available from the repo root:

- `yarn typecheck` — run TypeScript without emitting
- `yarn lint` — run ESLint over the codebase
- `yarn test` — run the Jest test suite
- `yarn prepare` — build the library (commonjs, ESM and `.d.ts`) via `react-native-builder-bob`
- `yarn example <script>` — run any script from the `example` workspace (e.g. `yarn example android`)

Please make sure `yarn typecheck`, `yarn lint` and `yarn test` all pass before opening a pull request.

## Commit messages

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. This is enforced by `commitlint` via a git hook.

Examples:

- `feat: support reading DG11 data group`
- `fix(android): handle transceive timeout gracefully`
- `docs: clarify iOS entitlement setup`
- `chore: bump example to react-native 0.85.2`

The commit type (`feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`, ...) is used to drive release notes.

## Pull requests

1. Fork the repo and create a branch from `main`.
2. Make your changes, keeping the diff focused on a single concern.
3. If you change behaviour, update or add tests and update the README / API docs as needed.
4. Ensure `yarn typecheck && yarn lint && yarn test` all pass.
5. Open a PR against `main` with a clear description of *what* and *why*. Link any related issues.

For non-trivial changes, please open an issue first to discuss the approach before investing a lot of time.

## Reporting issues

When filing a bug report, include:

- The version of the library, React Native, and the OS/device you're on
- A minimal reproduction (ideally based on the `example/` app)
- Relevant logs (Metro, Android `logcat`, Xcode console)
- Whether you're on the New Architecture or the legacy bridge

## Code of Conduct

Be kind and respectful. We follow the spirit of the [Contributor Covenant](https://www.contributor-covenant.org/).

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](./LICENSE) that covers the project.
