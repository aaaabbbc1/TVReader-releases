# TV Reader (Android / Jetpack Compose)

A couch- and remote-first e-reader built natively in Kotlin and Jetpack Compose for Android TV, tablets, and phones.

## Features

- **Full D-pad & Touch Navigation**: Every button, tab, card, and list item supports D-pad focus navigation alongside standard touch interactions.
- **Supported Formats**:
  - **EPUB**: OPF metadata, cover art, NCX/NAV table of contents, spine chapters, and embedded images.
  - **FB2**: FictionBook 2 XML (`.fb2` and `.fb2.zip`) with cover binaries and section hierarchy.
  - **PDF**: Native `PdfRenderer` page rendering with Original and Adaptive page modes.
  - **CBZ**: Comic book archive image sequence viewer.
  - **TXT / MD / HTML**: Clean typography, chapter splitting, headings, blockquotes, and entity decoding.
- **Reading Experience**:
  - Page-by-page pagination (`TextMeasurer`) and Continuous Scroll modes.
  - Alternative document-style layout with clickable full-page Table of Contents.
  - Side-panel Table of Contents, page bookmarks across books, adjustable font size/family/line spacing, and Focus Mode.
  - **5 Display Themes**: Light, Dark, Sepia, AMOLED, and Astigma (with 4 reduced-contrast Astigma presets) plus brightness control.
- **Library & Sources**:
  - Persistent local Room library with sorting (Title, Author, Date Added, Last Opened), full-text search, and Continue Reading hero card.
  - System File Picker (`OpenDocument`), `ACTION_VIEW` ("Open with") support, Direct URL download, WebDAV, FTP, Project Gutenberg (Gutendex API), and Standard Ebooks (OPDS feed).
