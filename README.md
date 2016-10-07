# stine_calendar_bot (Q & A)

## What is STiNE?

Without further comment, here is the full original self-description from STiNE as described here: [STiNE FAQ](https://www.stine.uni-hamburg.de/scripts/mgrqispi.dll?APPNAME=CampusNet&PRGNAME=EXTERNALPAGES&ARGUMENTS=-N000000000000002,-N000498,-Afaq "STiNE FAQ")

> STiNE is Universität Hamburg’s campus management system (CampusNet®). Developed by Datenlotsen Informationssysteme GmbH, it serves a variety of functions. STiNE combines all essential teaching and study processes into one single IT platform and database that all those involved in these processes—students, teachers and administrators—are able to access according to their user roles and rights.

## What is the purpose of this application?

STiNE is also able to export appointments as calendar files. Unfortunately, this has a few flaws:
- Calendars are not offered as an online calendar. Manual export is mandatory.
- Exported files can only contain appointments for one month max.
- At the moment, the ICS files are UTF-16-encoded. Nobody knows why or since when.
- Months that lie in the past cannot be exported.

This application automatically downloads available calendars and caches them. All available calendars (past, present and future) can then be merged into a single ICS file.

## What is the current release?

### [v0.0.3](https://github.com/felsenhower/stine_calendar_bot/releases/tag/v0.0.3)
> Fixed issues on the help screen.<br/>
Improved character encoding handling for German localisation.<br/>
Added version identifier to help screen.<br/>
Changed project url to github page.<br/>
Code cleanups.

## How do I run stine_calendar_bot?

Binaries can be downloaded from the [releases page](https://github.com/felsenhower/stine_calendar_bot/releases).

You will need a working installation of Java 1.8.

Start the application with `java -jar stine_calendar_bot.jar [options]` or `./stine_calendar_bot.jar [options]` (not on Windows).

A simple execution might look like this:

```
java -jar stine_calendar_bot.jar --language=de --user=baqxxxx --pass=password
```
The calendar cache directory will default to "./calendar_cache" and the output willd default to "./stine_calendar.ics" relative to the working directory.

You can avoid having your password get saved in your .bash_history in these two ways:
- Use `java -jar stine_calendar_bot.jar --language=de --user=baqxxxx --pass=--` and get queried for your password.
- By storing it in a password manager:
    - Install `gnome-keyring-query`
    - Store the password with `echo password | gnome-keyring-query set stine`
    - Call this application with:
    ```
    gnome-keyring-query get stine | \
    java -jar stine_calendar_bot.jar --language=de --user=baqxxxx --pass=--
    ```
    
Use `java -jar stine_calendar_bot.jar --help` for more information.

I have only tested this with Linux. If it successfully works on Windows and Mac as well, be so kind as to tell me.

There is no GUI implemented or planned.

## What languages does this application support?

As of september 2016, STiNE appears to be completely available in English as well as German. stine_calendar_bot will try to detect your default language and set it to German if applicable, and to English in all other cases. This changes the language of the exported files, the displayed page contents if explicitly echo'ed and the application's localisation. You can overwrite this with the `--language=`-Option which is strongly recommended in case something goes badly wrong.

## How do I build stine_calendar_bot?

Use the following tools:
- git (optional)
- java 1.8
- maven
- make (optional)

Source can be downloaded and compiled with:
```
git clone https://github.com/felsenhower/stine_calendar_bot.git
cd stine_calendar_bot
make all
```
Instead of `make all` you can also use `mvn package shade:shade` respectively.
