# Contributing to WLTechBlog Teleprompter

## Development Workflow

### Building Locally

1. Clone the repository
2. Set up your `local.properties` file with your Android SDK path
3. Build the debug APK: `./gradlew assembleDebug`

### Making Changes

1. Create a new branch for your feature or bugfix
2. Make your changes
3. Test thoroughly
4. Submit a pull request to the main branch

## Release Process

### Automated Builds

The project uses GitHub Actions for automated builds:

- **build.yml** - Runs on every push and pull request to main, building a debug APK
- **release.yml** - Runs when creating a release, building and attaching the APK as a release asset

### Creating a Release

1. Ensure all desired changes are merged to main
2. Create a new release in GitHub:
   - Go to "Releases" → "Create a new release"
   - Create a new tag (e.g., v1.0.0)
   - Write release notes
   - Click "Publish release"
3. The GitHub Action will automatically:
   - Build the debug APK
   - Upload it as a release asset

### Release Tags

We use semantic versioning for tags:
- `v1.0.0` - Major release
- `v1.1.0` - Minor release with new features
- `v1.0.1` - Patch release with bug fixes

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions focused and concise

## Testing

- Test all changes on physical Android devices when possible
- Verify features work in both portrait and landscape orientations
- Test with various .txt file sizes and formats
