# manbot

A simple bot to post man page links. And other stuff, probably.

## Usage

Get a discord api token and put it in a file `discord-token.txt` in the project root.
Build with [lein](https://leiningen.org/)

    $ lein uberjar
    $ java -jar target/uberjar/manbot-0.1.0-standalone.jar

You'll need to add it to a discord server after.

Get man page results by typing: `!man command` or `!manall command` into discord depending on whether you want the first man page or all of them sent.

