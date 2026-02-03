# HyuAuth

Authentication plugin for Hytale servers with registration and login support through UI windows.

## Features
- Registration and authentication through UI windows
- Data storage in SQLite database
- Password hashing using BCrypt
- Session system based on IP and player nickname
- Automatic authentication on reconnection within session time
- Authentication timeout with automatic kick
- Administrative commands for account management

## Requirements

- Java 25
- Hytale Server

2. Copy the JAR file to the `mods/` folder of your Hytale server

3. Restart the server

## Configuration

Configuration file is automatically created in the plugin folder: `config.yml`

```yaml
settings:
  login_timeout_seconds: 60
  session_timeout_minutes: 5
  database:
    file: "auth.db"
```

### Parameters

- `login_timeout_seconds` - time in seconds that a player has to authenticate before being kicked (default: 60)
- `session_timeout_minutes` - time in minutes during which a session is valid for automatic authentication (default: 5)
- `database.file` - SQLite database file name (default: "auth.db")

## Usage

### For Players

On first connection to the server, players are shown a registration window. After registration, subsequent logins show a login window.

If a player connects from the same IP and with the same nickname within the session time (default 5 minutes), authentication happens automatically.

### Commands for Administrators

- `/authreset <player>` - reset the account of the specified player (requires `hytale.admin` permission)
- `/authconfig <seconds>` - set authentication timeout in seconds (requires `hytale.admin` permission)

## Security

- Passwords are stored as BCrypt hashes (12 rounds)
- Passwords are never transmitted or stored in plain text
- Minimum password length: 3 characters
- Maximum password length: 64 characters

## Database Structure

Table `users`:
- `uuid` (TEXT, PRIMARY KEY) - player UUID
- `username` (TEXT, NOT NULL) - player name
- `password_hash` (TEXT, NOT NULL) - BCrypt password hash
- `created_at` (INTEGER, NOT NULL) - account creation time in milliseconds
