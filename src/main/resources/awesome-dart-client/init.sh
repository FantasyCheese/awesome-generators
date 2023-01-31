find . -name "*200_response*" -delete
grep -v "200_response" lib/model/_index.dart > temp && mv temp lib/model/_index.dart
flutter pub get
flutter pub run build_runner build --delete-conflicting-outputs
flutter format .
