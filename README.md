# Notion to bookmarks

This little webapp will generate a JSON bookmarks document from a Notion page.

This is intended to be used with [bbt](https://github.com/BoD/bbt).

## How to use

### TL;DR
https://<span></span>server/`NOTION_COOKIE`/`PAGE_ID`

### In more words

You need to craft a URL of this form:

https://<span></span>server/`NOTION_COOKIE`/`PAGE_ID`

where:

- `NOTION_COOKIE` is your personal Notion cookie value used to
identify you and allow accessing the Notion API.
- `PAGE_ID` is the id of the page that you want to turn into bookmarks

#### How to get your Notion cookie?

1. Go to https://www.notion.so with Chrome and make sure you're logged in
2. Find the value of the cookie named `token_v2`, by following the
   instructions [here](https://developer.chrome.com/docs/devtools/storage/cookies/).

#### How to get your page id?
The page id is the **last part** of the URL of the Notion page you're interested in.

So for instance, given this Notion page:

https://www.notion.so/mycompany/Very-interesting-page-99b251d56eaf46bd91df184f11607f8c

The page id is `99b251d56eaf46bd91df184f11607f8c`.

As you can see it is a shorter string of digits and letters.

#### And of course

Obviously, do not pick a page that has too many sub pages (for instance,
the root folder of your whole Notion instance), the resulting bookmark
document would be too deep!

## Docker instructions

### Building and pushing the image to Docker Hub

```
docker image rm bodlulu/notion-to-bookmarks:latest
DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage
```

### Running the image

```
docker pull bodlulu/notion-to-bookmarks
docker run -p <PORT TO LISTEN TO>:8080 bodlulu/notion-to-bookmarks
```

## Licence

Copyright (C) 2020-present Benoit 'BoD' Lubek (BoD@JRAF.org)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.
