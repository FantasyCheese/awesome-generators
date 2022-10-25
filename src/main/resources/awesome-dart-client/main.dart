import 'package:dio/dio.dart';
import 'package:openapi/api/default_api.dart';
import 'package:flutter/material.dart';
import 'package:openapi/model/_index.dart';

void main() async {
  runApp(MaterialApp(
    home: Home(),
  ));
}

class Home extends StatelessWidget {
  const Home({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final dio = Dio();
          final client = RestClient(dio, baseUrl: 'https://api.mdtvdevelop.com/api/v1/admin');
          final result = await client.adminLogin(Login(username: 'joshua', password: 'qwer1234'));
          print(result);
        },
      ),
    );
  }
}
