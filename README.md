# Feedback agent
This is the backend for the feedback agent part of the metadata feedback system. It 
allows biobank administrators to associate metadata (e.g. the DOIs of relevant publications) with
locally held samples.

It uses a Postgres database to store the associations between specimens and metadata, in
a table called ```specimen_feedback```. Various parameters can be configured via environment
variables, see the ```docker-compose.yml``` file for examples.

## Building for production
The feedback agent is designed to be run within a [Bridgehead](https://github.com/samply/bridgehead).
Before you start your Bridgehead, you will need to build a Docker container for this component:

``` code
git clone https://github.com/samply/feedback-agent.git
cd feedback-agent
mvn clean install
docker build -t samply/feedback-agent .
```

## Running locally
If you just want to try out an isolated feedback agent (not in a Bridgehead), you can run a test instance locally.

First build the backend app by running
``` code
mvn clean package -DskipTests
```

To start the application the enviroment variables in docker-compose file need to be set up to connect feedback-agent to the already existing beam network, the blaze-store running in the Bridgehead and the Feedback hub, for example:
 
``` code
      - BEAM_PROXY_URI=http://192.168.0.109:8082
      - FEEDBACK_HUB_URL=http://192.168.0.109:8071
      - BLAZE_BASE_URL=http://192.168.0.100:8091/fhir
```

Then run and access the user interface through Bridgehead UI
`docker-compose up -d  feedback-agent-be feedback-agent-ui`
