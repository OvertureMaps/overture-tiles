FROM amazonlinux

# Download tippecanoe deps and Java 22+ for single-file .java Planetiler profiles, for large themes.

RUN yum update -y && yum install -y tar make gzip gcc-c++ sqlite-devel zlib-devel java-22-amazon-corretto-headless && yum clean all

# Build tippecanoe for creating tilesets for small themes.

RUN curl -L https://github.com/felt/tippecanoe/archive/refs/tags/2.55.0.tar.gz | tar xz -C /opt/
WORKDIR /opt/tippecanoe-2.55.0

RUN make && make install

RUN rm -r /opt/tippecanoe-2.55.0

# download and install duckdb for reading Overture Parquet files.

RUN curl -L https://github.com/duckdb/duckdb/releases/download/v0.10.2/duckdb_cli-linux-aarch64.zip -o duckdb_cli-linux.zip && unzip duckdb_cli-linux.zip -d /usr/local/bin/ && rm duckdb_cli-linux.zip

RUN duckdb -c "install httpfs; install spatial;"

# Download and install aws cli for authenticated uploads to S3 buckets.

RUN curl -L https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip -o awscliv2.zip && unzip awscliv2.zip && ./aws/install && rm awscliv2.zip && rm -r ./aws

# copy current scripts into image.

COPY scripts /scripts

ENTRYPOINT ["sh","/scripts/2024-06-13-beta.0/places_full.sh"]