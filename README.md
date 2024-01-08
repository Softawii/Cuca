# Cuca

Cuca is a Discord-bot to authenticate users on a Discord-server using specific domain email-addresses.

# Installation

- Gradle
- Java 21
- Micronaut
- JDA
- Postgres

```agsl
--discord_token=**** 
--mail.from=xyz@wkd.br 
--mail.username=xyz@wkd.br 
--mail.password="xxxx yyyy zzzz wwww" 
--html_template_path="D:\source\cuca\src\main\resources\email-template\index.html" 
--app.discord.event_channel=981332113645240351 
--curupira.reset=true 
--dev_env=true
```

# Contributing

You can contribute to Cuca by forking this repository and creating a pull-request. Please make sure to follow the [Contribution Guidelines](./docs/CONTRIBUTING.md).

# Features

- [x] Authentication using email-addresses
- [x] One discord-account per email-address
- [x] Bruteforce protection
- [x] Automatic role-assignment
- [x] Automatic role-removal
- [x] Manual role-assignment
- [x] Manual role-removal
- [x] Gmail support
- [x] Event logging
- [x] User management

# Video

[![](https://markdown-videos-api.jorgenkh.no/youtube/OihAeT5wYpQ)](https://youtu.be/OihAeT5wYpQ)