FROM curlimages/curl:latest
COPY init-models.sh /init-models.sh
RUN chmod +x /init-models.sh
ENTRYPOINT ["/init-models.sh"]