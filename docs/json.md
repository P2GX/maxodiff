# 🛠️ Webserver: HTML/JSON Output 

The **maxodiff** server can output JSON payloads. 

!!! info "Pilot Phase"
    The JSON output feature is currently in a pilot stage. We are actively seeking feedback to refine the data structure!

---

## 🚀 Server Setup

Before testing the JSON output, ensure your local environment is configured. For a full deep-dive, see the [Setup Guide](setup.md).
After your environment is setup, run the HTML module to host the web interface:

```shell
java -jar maxodiff-html/target/maxodiff-html.jar
```


Once started, navigate to:

👉 http://localhost:8080/maxodiff

To run the web interface, follow these steps:

- Select a phenopacket using the Sample Input box (on the top)
- Set the output to **HTML** or **JSON** as desired
- Click the **RUn differential diagnosis calculation** button
- You should then see a progress bar. 


On our laptops, `maxodiff` needs about 20 seconds to load the IC file (which only needs ot happen when the server is first started), and then takes about 30 seconds to run the actual algorithm. The time depends on the number of repetitions and then number of terms in the phenopacket.

!!! success "JSON output mode"
    The server will bypass the standard Thymeleaf HTML rendering and return a raw JSON stream. You should see the structured data directly in your browser window.

!!! success "HTML output mode"  
    You should see an HTML page with tables and graphics that illustrate the result (TODO add documentation when final SAMS version is ready!)


  