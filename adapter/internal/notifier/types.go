package notifier

// DeployedAPIRevision represents Information of deployed API revision data
type DeployedAPIRevision struct {
	APIID      string            `json:"apiId"`
	RevisionID int               `json:"revisionId"`
	EnvInfo    []DeployedEnvInfo `json:"envInfo"`
}

// DeployedEnvInfo represents env Information of deployed API revision
type DeployedEnvInfo struct {
	Name  string `json:"name"`
	VHost string `json:"vhost"`
}
