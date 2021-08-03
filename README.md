# Astro Discord Bot

## Information

This project is NOT official from Astro-Life!
This discord bot was created so that GTA-RP departments (pre-built supported are the Medical-Department and the Police-Department of the server Astro-Life where i play on (it technically works independent of the server) can manage their employees over discord with ease, and nearly all messages are customizable. The bot almost fully works with slash-commands!

## Features

### Employee management
- Promote and demote employees
- Manage additional educations/trainings
- Manage special forces
- Manage warns
- Termination

### Employee information
Additionally to the points above the bot also saves and provides the following data:
- Worktime (How much time did employees spend working)
- Phone numbers
- Birth dates

### Automatic logs
The bot automatically writes logs for the following events:
- Promotion / demotion of an employee
- When new employees joins the department
- When employees get terminated or quit by themselves
- When employees join/leave patrols

### Vacation management
The bot can know about a channel where employees can write vacation requests, and members with specific configurable roles can accept / decline these requests.

### Patrol system
The bot also has a patrol-system, where employees can join different patrols and set the patrols vehicle, special force and status code.

## Commands
- /register <mention> <name> [phone number] [birth date]: This command is used to register already existing employees before using the bot into the database.
- /azubi(configurable command name) <phone number> <birth date>: This command is used to register yourself into the database and get all the needed roles after joining the discord server.
- /info <mention>: This command is used by admins to manage employees.
- /memberlist: This command is used by admins to see a full employee list.
- /terminate(configurable command name) <mention> <reason>: This command is used to terminate employees.
- /update <phone number> <birth date>: This command can be used to update personal data (phone number and birth date).
  
## Screenshots

### Patrol system
![grafik](https://user-images.githubusercontent.com/59053718/128013425-95aec807-6827-4062-8219-603dfc54f44a.png)

### Employee management
![grafik](https://user-images.githubusercontent.com/59053718/128013766-ecb480ae-db1c-4d6e-b780-e563ef8cac87.png)

### Logs
![grafik](https://user-images.githubusercontent.com/59053718/128014058-91460032-9611-4067-9d93-1efd8b47aa00.png)

## Building

Execute build.sh or build.bat depending on your system

## Running

Example running command:
`java -Dfile.encoding=utf-8 -jar astro-discord-bot-1.0-all.jar <YOUR BOT TOKEN HERE> <DEPARTMENT HERE (pre-built are md/pd)> <GUILD ID TO RUN ON HERE>`
