# Use an official OpenJDK runtime as a parent image
FROM openjdk:8

# Install make, wget, and gettext (for envsubst)
RUN apt-get update && apt-get install -y \
    make \
    wget \
    gettext

# Set the working directory in the container
WORKDIR /app

# Copy the project files into the container
COPY . /app

# Run the Makefile to download dependencies and build the project
RUN make build

# Command to run when the container starts (optional)
CMD ["java", "-jar", "dl/SweetHome3D-7.4.jar"]