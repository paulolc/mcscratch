FROM docker.io/openjdk/8

# Install the application
ADD dist /mc 
ENV MC_PORT 25565
EXPOSE 25565
RUN cd /mc
# Define command to run the application when the container starts
CMD ["java", "-Xmx1024M", "-Xms1024M", "-jar", "spigot*.jar","nogui"] 