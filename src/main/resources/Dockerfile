FROM registry.ng.bluemix.net/ibmliberty

# Install the application
ADD . /mc 
ENV MC_PORT 25565
EXPOSE 25565
WORKDIR /mc
# Define command to run the application when the container starts
RUN chmod 755 start-mcscratch.sh 
CMD ./start-mcscratch.sh
