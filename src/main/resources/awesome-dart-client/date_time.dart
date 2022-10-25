import 'dart:core' as core;

class DateTime extends core.DateTime {
  DateTime(super.year);
  core.String toJson() => toIso8601String();
}
