# 🛠️ JSON Output (Beta)

We are implementing code to allow the **maxodiff** server to output JSON payloads. This transition enables our analysis engine to be consumed by external front-end applications and automated pipelines.

!!! info "Pilot Phase"
    The JSON output feature is currently in a pilot stage. We are actively seeking feedback to refine the data structure!

---

## 🚀 Server Setup

Before testing the JSON output, ensure your local environment is configured. For a full deep-dive, see the [Setup Guide](setup.md).

### 1. Build and Prepare
Use the tabs below to follow the initialization steps:

=== "Step 1: Compile"
    Build the project using Maven:
    ```shell
    mvn clean package
    ```

=== "Step 2: Data Download"
    Download the necessary input data:
    ```shell
    java -jar maxodiff-cli/target/maxodiff-cli.jar download
    ```

### 2. Start the Server
Run the HTML module to host the web interface:

```shell
java -jar maxodiff-html/target/maxodiff-html.jar
```


Once started, navigate to:

👉 http://localhost:8080/maxodiff

🧪 Requesting JSON Results
To test the new programmatic output, follow these steps in the web interface:

Toggle Output Mode: In the configuration panel, set the output type to JSON.

Upload Phenopacket: Select your target phenopacket file.

Run Analysis: Click the start button.

!!! success "Expected Result"
  The server will bypass the standard Thymeleaf HTML rendering and return a raw JSON stream. You should see the structured data directly in your browser window.