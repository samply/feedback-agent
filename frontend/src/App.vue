<template>
  <div class="container text-center  mt-5 mb-5">
    <h1 class="mt-5 fw-bolder text-success "> Feedback Agent </h1>
    <p>
      This page is designed to link issued specimen from this bridgehead with ID of sample request.
       In the table there are local data displayed based on the provided query.
       To link specimen with request ID select specimen by clicking checkbox in corresponding row of the table,
       enter the request ID into provided input form and click on "Submit" button.
    </p>
  </div>
  <div class="container">
    <div class="row mb-2">
      <div class="col-10">
        <input class="form-control" type="text" v-model="query" placeholder="FHIR-Query">
      </div>
      <div class="col-2">
        <button type="button" class="btn btn-primary" @click="fetchSpecimen(this.query)">
          <i class="bi bi-search" aria-label='Search Icon'></i>
          Browse by query
        </button>
      </div>
    </div>
    <div class="row mb-2">
      <div class="col-12">
        <div class="table-wrapper table-responsive">
          <CheckboxTable v-if="file.length > 0" ref="checkBoxTable" :fields="fields" :tableData="file"></CheckboxTable>
        </div>
        <div class="d-flex justify-content-center">
          <div class="spinner-border text-primary" role="status" v-if="loading">
            <span class="sr-only">Loading...</span>
          </div>
        </div>
      </div>
    </div>
    <div class="row mb-4">
      <div class="col-7">
        <input class="form-control" type="text" v-model="requestID" placeholder="Request-ID">
      </div>
      <div class="col-2">
        <button type="submit" class="btn btn-success" @click="postSelected()" :disabled="requestID ==''">
          <i class="bi-file-earmark-medical" aria-label='Submit Icon'></i>
          Submit
        </button>
      </div>
      <div class="col-3">
        <div class="alert alert-success" role="alert" v-if="enableSuccessAlert">
          Selected entries uploaded successfully.
        </div>
        <div class="alert alert-danger" role="alert" v-if="enableDangerAlert">
          Failed to uploaded selected entries.
        </div>
      </div>
    </div>
  </div>
</template>
<script>
// Importing http client
import axios from 'axios'
// Importing the table component
import CheckboxTable from './components/CheckboxTable.vue'

export default {
  name: 'App',
  components: {
    CheckboxTable,
    //VueGoodTable
  },
  data() {
    return {
        query: "Specimen?subject=Patient/bbmri-18&type=rna",
        requestID : "",
        x_api_key: 'ttsHGwSITs0Eq8L63YWtLVyHymBmULvZIihL6w4t42FBmzp6Eb9SGNd7fZmeUtAI',
        exportTemplate: "bbmri-feedback-agent",
        file: "",
        content: [],
        parsed: false,
        enableSubmit: false,
        enableSuccessAlert: false,
        enableDangerAlert: false,
        fields: [],
        loading: false
    }
  },
  methods: {
    fetchSpecimen() {
      this.content.data = [];
      this.parsed = false;
      axios.defaults.headers['X-API-KEY'] = this.x_api_key;
      
      // Encode the query string before appending it to the URL
      const encodedQuery = encodeURIComponent(this.query);

      // Construct the URL with the encoded query
      const requestUrl = `http://localhost:8092/request?query=${encodedQuery}&query-format=FHIR_QUERY&template-id=${this.exportTemplate}&output-format=JSON`;

      axios.post(requestUrl)
        .then(response => {
          this.pollForJSON(response.data.responseUrl);
        });
    },
    
    pollForJSON(JSONUrl) {
      axios.defaults.headers['X-API-KEY'] = null;
      const pollInterval = 500; // 500 milliseconds
      const maxPollAttempts = 10;
      let pollAttempts = 0;
      this.file = "";
      this.loading = true;

      const poll = () => {
        axios.get(JSONUrl)
          .then(response => {
            if (response.status === 200 && response.data) {
              console.log(response);
              this.file = response.data.Samples.map((sample) => ({
                ...sample,
                sampleID: sample.sampleID.replace('Specimen/', ''),
              }));
              console.log(this.file);
              console.log("file should be updated");
              if (this.file.length > 0) {
                // Extract keys from the first object in the Samples array
                this.fields = Object.keys(this.file[0]);
              }

              // Stop polling since the response is available
              this.loading = false;
              return;
            }

            // Increment pollAttempts and check if we have reached the maximum attempts
            pollAttempts++;
            if (pollAttempts >= maxPollAttempts) {
              console.log("Exceeded maximum poll attempts");
              this.loading = false;
              return;
            }

            // Retry after the polling interval
            setTimeout(poll, pollInterval);
          })
          .catch(error => {
            console.error("Error while polling for JSON:", error);
            // Retry after the polling interval
            setTimeout(poll, pollInterval);
          });
      };

      poll();
    },
    postSelected(){
      let postData = this.$refs.checkBoxTable.selected();
      postData.forEach((element) => {
          element.requestID = this.requestID;
      });
      axios.post("http://localhost:8072/multiple-specimen-feedback", { "feedbackList" : postData })
      .then(response => {
          if (response.status == 200) {
            this.enableSuccessAlert = true;
            this.enableDangerAlert = false;
          } else {
            this.enableSuccessAlert = false;
            this.enableDangerAlert = true;
          }
      });
    }
  },
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: left;
  color: #2c3e50;
}
.table-wrapper {
  max-height: 500px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0,0,0,0);
  border: 0;
}
</style>
