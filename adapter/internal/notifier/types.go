package notifier

// DeployedAPIRevision represents Information of deployed API revision data
type DeployedAPIRevision struct {
	APIID      string            `json:"apiID"`
	RevisionID int               `json:"revisionID"`
	EnvInfo    []DeployedEnvInfo `json:"envInfo"`
}

// DeployedEnvInfo represents env Information of deployed API revision
type DeployedEnvInfo struct {
	Name  string `json:"name"`
	VHost string `json:"vhost"`
}
