#!/bin/sh

# target file name
TARGET=spec

build_pdf() {
    echo Building the target pdf file with pandoc
    pandoc \
        --pdf-engine=xelatex \
        -o $TARGET.pdf \
        --template=eisvogel \
        --bibliography=bibliography.bib \
        --csl=bibliography.csl \
        -s \
        -f markdown \
        $TARGET.md \
        metadata.yaml
}

format_markdown() {
    echo Running pandoc to lint and format the text
    pandoc --atx-headers -f markdown -t markdown -o temp.md $TARGET.md
    rm $TARGET.md
    mv temp.md $TARGET.md
}

# main
case $1 in
build)
    build_pdf
    ;;
format)
    format_markdown
    ;;
*)
    format_markdown || exit 1
    build_pdf
    ;;
esac
