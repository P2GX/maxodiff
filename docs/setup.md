# setup


maxodiff is a Java application. To build it from source, you will need to have installed Apache [maven](https://maven.apache.org/)
and [Java Development Kit (JDK)](https://openjdk.org/).


## todo
detailed instructions





## Edit documentation

To edit the mkdocs documentation, create a virtual envelop and install the following packages

``` bash
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install mkdocs-material
pip install pillow cairosvg
pip install mkdocs-material-extensions
pip install mkdocs-material[imaging]
pip install mkdocstrings[python]
```

Then start the server.


``` bash
mkdocs server
```

The website will be available at and go to http://127.0.0.1:8000/.