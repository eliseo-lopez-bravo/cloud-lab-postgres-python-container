terraform {
  backend "oci" {
    bucket         = "terraform-state-bucket"
    compartment_id = "ocid1.tenancy.oc1..aaaaaaaad4ipuawmwb2miwp3bosu6i6ufmkkbvh572ceabiesraziz6zhb7q"    
    namespace      = "axqetqkbgelv"
    region         = "us-sanjose-1"
    key            = "terraform.tfstate"
  }
}
