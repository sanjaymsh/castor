CREATE TABLE LOG (
            LOG_ID          INT              NOT NULL,
            LOG_TIMESTAMP   TIMESTAMP        NOT NULL,
            LOG_CLASS       VARCHAR  ( 100)  NOT NULL,  
            LOG_LEVEL       VARCHAR  (  10)  NOT NULL,  
            LOG_THREAD      VARCHAR  ( 100)  NOT NULL,  
            LOG_MESSAGE     VARCHAR  (1000)  NULL,
            LOG_COUNT       INT              NOT NULL
);

ALTER TABLE LOG ADD PRIMARY KEY (LOG_ID);

CREATE TABLE LOG_EXCEPTION (
            LOGE_ID         INT              NOT NULL,
            LOGE_LOG_ID     INT              NOT NULL,
            LOGE_STACKTRACE TEXT             NOT NULL
);

ALTER TABLE LOG_EXCEPTION ADD PRIMARY KEY (LOGE_ID);

ALTER TABLE LOG_EXCEPTION ADD CONSTRAINT FK_LOGE_LOG_ID
FOREIGN KEY (LOGE_LOG_ID) REFERENCES LOG (LOG_ID);

CREATE TABLE LOG_EXTENSION (
            LOGX_LOG_ID    INT              NOT NULL,
            LOGX_TYPE      VARCHAR  ( 100)  NOT NULL,  
            LOGX_VALUE     VARCHAR  ( 100)  NOT NULL
);

ALTER TABLE LOG_EXTENSION ADD PRIMARY KEY (LOGX_LOG_ID);

ALTER TABLE LOG_EXTENSION ADD CONSTRAINT FK_LOGX_LOG_ID
FOREIGN KEY (LOGX_LOG_ID) REFERENCES LOG (LOG_ID);

commit;